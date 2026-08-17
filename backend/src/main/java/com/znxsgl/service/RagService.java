package com.znxsgl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znxsgl.entity.DocumentVector;
import com.znxsgl.mapper.DocumentVectorMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final DocumentVectorMapper docMapper;
    private final LlmService llmService;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbc;
    private final Path uploadDir;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public RagService(DocumentVectorMapper docMapper, LlmService llmService,
                      EmbeddingService embeddingService, JdbcTemplate jdbc) {
        this.docMapper = docMapper;
        this.llmService = llmService;
        this.embeddingService = embeddingService;
        this.jdbc = jdbc;
        try {
            uploadDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "znxsgl_uploads"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 上传并分析文件，存入向量库（语义向量化） */
    public String uploadAndAnalyze(String courseName, MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            Path saved = uploadDir.resolve(UUID.randomUUID() + "_" + fileName);
            file.transferTo(saved.toFile());

            String extractedText;

            if (isDocument(fileName)) {
                extractedText = extractDocumentText(saved);
            } else {
                return "（暂不支持的文件类型：仅支持 PDF、DOCX、TXT、MD）";
            }

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return "（文件已上传，但未能提取到文字内容）";
            }

            // 分块
            List<String> chunks = splitText(extractedText, 500);

            // 批量向量化
            float[][] embeddings = embeddingService.embedBatch(chunks);

            // 存入数据库
            for (int i = 0; i < chunks.size(); i++) {
                DocumentVector dv = new DocumentVector();
                dv.setCourseName(courseName);
                dv.setDocName(fileName);
                dv.setContentChunk(chunks.get(i).trim());
                // 存储向量为 JSON 数组字符串
                if (embeddings != null && i < embeddings.length && embeddings[i] != null) {
                    dv.setEmbedding(vectorToJson(embeddings[i]));
                }
                dv.setCreatedAt(LocalDateTime.now());
                docMapper.insert(dv);
            }

            String summary = chunks.size() > 1
                    ? extractedText.substring(0, Math.min(200, extractedText.length())) + "..."
                    : extractedText;
            return "📎 已分析《" + fileName + "》，提取 " + extractedText.length()
                    + " 字，分 " + chunks.size() + " 块存入知识库"
                    + (embeddings != null ? "（语义向量已生成）" : "") + "：\n\n" + summary;
        } catch (Exception e) {
            e.printStackTrace();
            return "（文件处理失败：" + e.getMessage() + "）";
        }
    }

    /**
     * 语义向量检索：将问题向量化后，与知识库中所有文档块计算余弦相似度，取 top-5
     * 如果向量检索不可用，降级为关键词 LIKE 匹配
     */
    public String retrieveContext(String courseName, String question) {
        try {
            // 1. 将用户问题向量化
            float[] queryVec = embeddingService.embed(question);
            if (queryVec == null) {
                System.out.println("=== Embedding 失败，降级为关键词检索");
                return retrieveContextFallback(courseName, question);
            }

            // 2. 加载该课程所有带向量的文档块
            List<DocumentVector> all = docMapper.findByCourseWithEmbedding(courseName);
            if (all == null || all.isEmpty()) {
                // 没有向量数据，降级
                return retrieveContextFallback(courseName, question);
            }

            // 3. 计算余弦相似度并排序
            List<DocScore> scored = new ArrayList<>();
            for (DocumentVector dv : all) {
                float[] docVec = jsonToVector(dv.getEmbedding());
                if (docVec == null) continue;
                double sim = cosineSimilarity(queryVec, docVec);
                scored.add(new DocScore(dv.getContentChunk(), sim));
            }
            scored.sort((a, b) -> Double.compare(b.score, a.score));

            // 4. 取 top-5
            List<String> topResults = scored.stream()
                    .limit(5)
                    .filter(ds -> ds.score > 0.3) // 过滤低相关度
                    .map(ds -> ds.text)
                    .collect(Collectors.toList());

            if (topResults.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("以下是课程相关参考内容（语义检索）：\n");
            for (int i = 0; i < topResults.size(); i++) {
                sb.append("---\n参考").append(i + 1).append("：").append(topResults.get(i)).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            System.out.println("=== 向量检索异常，降级关键词: " + e.getMessage());
            return retrieveContextFallback(courseName, question);
        }
    }

    /** 降级方案：关键词 LIKE 匹配（兼容旧数据） */
    private String retrieveContextFallback(String courseName, String question) {
        try {
            // 查 content_chunk 做简单的关键词提取
            List<DocumentVector> all = docMapper.findByCourseWithEmbedding(courseName);
            if (all == null || all.isEmpty()) {
                // 没有带 embedding 的数据，直接用旧方式查所有
                List<DocumentVector> raw = docMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocumentVector>()
                                .eq(DocumentVector::getCourseName, courseName)
                                .last("LIMIT 20"));
                if (raw == null || raw.isEmpty()) return "";

                // 简单关键词匹配
                List<String> results = new ArrayList<>();
                for (DocumentVector dv : raw) {
                    if (dv.getContentChunk() != null && dv.getContentChunk().contains(question.substring(0, Math.min(10, question.length())))) {
                        results.add(dv.getContentChunk());
                    }
                }
                if (results.isEmpty()) {
                    results = raw.stream().map(DocumentVector::getContentChunk).limit(3).collect(Collectors.toList());
                }

                StringBuilder sb = new StringBuilder("以下是课程相关参考内容：\n");
                for (int i = 0; i < Math.min(results.size(), 5); i++) {
                    sb.append("---\n参考").append(i + 1).append("：").append(results.get(i)).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            System.out.println("=== 降级检索也失败了: " + e.getMessage());
        }
        return "";
    }

    /**
     * 按章节ID列表检索内容（用于出题）
     * 简单策略：按课时分块（每个 lesson 一块），分块数<=8全部返回，>8用 subject 做相似度检索 top-8
     */
    public String retrieveByChapters(List<Long> chapterIds, String subject) {
        if (chapterIds == null || chapterIds.isEmpty()) return "";
        try {
            String placeholders = chapterIds.stream().map(c -> "?").collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT content_chunk, embedding FROM document_vector WHERE chapter_id IN (" + placeholders + ")",
                    chapterIds.toArray());

            // 降级：向量库无数据，直接查课时原文
            if (rows == null || rows.isEmpty()) {
                List<String> contents = jdbc.queryForList(
                        "SELECT content FROM course_lesson WHERE chapter_id IN (" + placeholders + ") " +
                        "AND content IS NOT NULL AND content != ''",
                        String.class, chapterIds.toArray());
                if (contents == null || contents.isEmpty()) return "";
                StringBuilder sb = new StringBuilder();
                for (String c : contents) sb.append(c).append("\n---\n");
                return truncate(sb.toString(), 8000);
            }

            // 分块数 <= 8，直接拼接
            if (rows.size() <= 8) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> r : rows) {
                    sb.append(r.get("content_chunk")).append("\n---\n");
                }
                return truncate(sb.toString(), 8000);
            }

            // > 8，用 subject 做向量相似度 top-8
            float[] queryVec = embeddingService.embed(subject);
            if (queryVec == null) {
                // embedding 失败，降级取前8个
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8 && i < rows.size(); i++) {
                    sb.append(rows.get(i).get("content_chunk")).append("\n---\n");
                }
                return truncate(sb.toString(), 8000);
            }
            List<DocScore> scored = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                float[] docVec = jsonToVector((String) r.get("embedding"));
                if (docVec == null) continue;
                double sim = cosineSimilarity(queryVec, docVec);
                scored.add(new DocScore((String) r.get("content_chunk"), sim));
            }
            scored.sort((a, b) -> Double.compare(b.score, a.score));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8 && i < scored.size(); i++) {
                sb.append(scored.get(i).text).append("\n---\n");
            }
            return truncate(sb.toString(), 8000);
        } catch (Exception e) {
            System.out.println("=== retrieveByChapters 异常: " + e.getMessage());
            return "";
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // ===== 向量工具方法 =====

    /** 余弦相似度 */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dot / denominator;
    }

    /** 向量 → JSON 字符串 */
    private String vectorToJson(float[] vec) {
        try {
            return jsonMapper.writeValueAsString(vec);
        } catch (JsonProcessingException e) {
            // fallback: 手动拼接
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < vec.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(vec[i]);
            }
            return sb.append("]").toString();
        }
    }

    /** JSON 字符串 → 向量 */
    private float[] jsonToVector(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return jsonMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 文本处理工具方法 =====

    private boolean isDocument(String name) {
        if (name == null) return false;
        String l = name.toLowerCase();
        return l.endsWith(".pdf") || l.endsWith(".docx") || l.endsWith(".doc")
                || l.endsWith(".txt") || l.endsWith(".md");
    }

    /** 从文档中提取文本 */
    private String extractDocumentText(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        try {
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return new String(Files.readAllBytes(file));
            }
            if (name.endsWith(".docx")) {
                return extractDocxText(file);
            }
            if (name.endsWith(".pdf")) {
                return extractPdfText(file);
            }
            if (name.endsWith(".doc")) {
                // 旧版 .doc 格式不做复杂处理，用 AI 分析
                return llmService.analyzeDocument("请提取这个文档的文本内容", file.toFile().getAbsolutePath(), null);
            }
        } catch (Exception e) {
            System.out.println("=== 文档提取失败: " + e.getMessage());
        }
        return null;
    }

    /** DOCX 文本提取（纯 ZIP + XML 解析，无需 POI） */
    private String extractDocxText(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new FileInputStream(file.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    String xml = new String(zis.readAllBytes());
                    // 简单提取 <w:t> 标签中的文本
                    String[] parts = xml.split("<w:t[^>]*>");
                    for (String part : parts) {
                        int end = part.indexOf("</w:t>");
                        if (end >= 0) sb.append(part, 0, end);
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    /** PDF 文本提取（纯 Java，无需 PDFBox） */
    private String extractPdfText(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder();

        // 提取 BT...ET 文本块
        int pos = 0;
        while ((pos = content.indexOf("BT", pos)) >= 0) {
            int end = content.indexOf("ET", pos);
            if (end < 0) break;
            String block = content.substring(pos + 2, end);
            // 提取 Tj 和 TJ 操作符中的文本
            int tjPos = 0;
            while ((tjPos = block.indexOf("Tj", tjPos)) >= 0) {
                int start = block.lastIndexOf("(", tjPos);
                int stop = block.indexOf(")", tjPos);
                if (start >= 0 && stop > start) {
                    sb.append(block, start + 1, stop);
                }
                tjPos += 2;
            }
            pos = end + 2;
        }
        return sb.toString().trim();
    }

    /** 文本分块 */
    private List<String> splitText(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            // 尽量在句子边界处断开
            if (end < text.length()) {
                int breakPoint = text.lastIndexOf('。', end);
                if (breakPoint > start && breakPoint > end - 100) end = breakPoint + 1;
                else {
                    breakPoint = text.lastIndexOf('\n', end);
                    if (breakPoint > start && breakPoint > end - 100) end = breakPoint;
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    /** 辅助类：文档块+相似度分数 */
    private static class DocScore {
        final String text;
        final double score;
        DocScore(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }
}
