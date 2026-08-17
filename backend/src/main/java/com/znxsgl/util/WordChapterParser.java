package com.znxsgl.util;

import com.znxsgl.dto.ChapterImportRowDTO;
import com.znxsgl.dto.ImportFailureDTO;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 课程章节导入解析器
 * 支持两种格式：
 * 1. 标题层级式：第 X 章 / X.Y 节
 * 2. 表格清单式：与 Excel 模板列名一致
 */
public class WordChapterParser {

    private static final List<String> VALID_RESOURCE_TYPES = Arrays.asList("video", "document", "quiz", "link", "text", "ppt", "pdf", "audio", "image");

    // 章节匹配：第1章 / 第一章 / 1. 名称（名称不能以数字开头，避免与 1.1 课时冲突）
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^(?:第\\s*([0-9]+)\\s*[章篇单元]|第\\s*([一二三四五六七八九十百千万]+)\\s*[章篇单元])\\s*(.+)$");

    // 数字章节：1. 名称 / 1、名称 / 1 名称（名称不能以数字开头）
    private static final Pattern NUMBER_CHAPTER_PATTERN = Pattern.compile(
            "^([0-9]+)\\s*[.、．]\\s+([^0-9].+)$");

    // 课时匹配：1.1 / 第1.1节 / 课时1
    private static final Pattern LESSON_PATTERN = Pattern.compile(
            "^(?:第\\s*([0-9]+)\\.([0-9]+)\\s*节|([0-9]+)\\.([0-9]+))\\s*[.、．]?\\s*(.+)$");

    // 课时 1 XXX
    private static final Pattern LESSON_NUMBER_PATTERN = Pattern.compile(
            "^课\\s*时\\s*([0-9]+)\\s*[.、．]?\\s*(.+)$");

    private final List<ImportFailureDTO> failures = new ArrayList<>();

    /**
     * 解析 Word 文件，返回所有行数据
     *
     * @param input Word 输入流
     * @return 章节导入行列表
     */
    public List<ChapterImportRowDTO> parse(InputStream input) {
        List<ChapterImportRowDTO> rows = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(input)) {
            // 优先尝试表格模式
            List<ChapterImportRowDTO> tableRows = parseTables(doc);
            if (!tableRows.isEmpty()) {
                rows.addAll(tableRows);
            } else {
                // 表格为空则按标题层级解析
                rows.addAll(parseHeadings(doc));
            }
        } catch (Exception e) {
            throw new RuntimeException("Word 解析失败：" + e.getMessage(), e);
        }
        return rows;
    }

    public List<ImportFailureDTO> getFailures() {
        return failures;
    }

    /**
     * 表格清单式解析
     */
    private List<ChapterImportRowDTO> parseTables(XWPFDocument doc) {
        List<ChapterImportRowDTO> result = new ArrayList<>();
        for (XWPFTable table : doc.getTables()) {
            List<XWPFTableRow> tableRows = table.getRows();
            if (tableRows.isEmpty()) {
                continue;
            }
            ColumnIndex idx = parseHeader(tableRows.get(0));
            if (idx == null || !idx.hasRequired()) {
                continue; // 不是章节导入表，跳过
            }
            for (int i = 1; i < tableRows.size(); i++) {
                XWPFTableRow row = tableRows.get(i);
                ChapterImportRowDTO dto = parseTableRow(row, idx, i + 1);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }
        return result;
    }

    /**
     * 标题层级式解析
     */
    private List<ChapterImportRowDTO> parseHeadings(XWPFDocument doc) {
        List<ChapterImportRowDTO> result = new ArrayList<>();
        List<IBodyElement> elements = doc.getBodyElements();

        int lastChapterNo = 0;
        int lastLessonNo = 0;
        Integer currentChapterNo = null;
        String currentChapterName = null;
        StringBuilder chapterDesc = new StringBuilder();
        Integer currentLessonNo = null;
        String currentLessonName = null;
        StringBuilder lessonContent = new StringBuilder();

        for (int i = 0; i < elements.size(); i++) {
            IBodyElement element = elements.get(i);
            if (element instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) element;
                String text = para.getText().trim();
                if (!StringUtils.hasText(text)) {
                    continue;
                }

                String style = para.getStyle();
                boolean isHeading1 = isHeading1(style, text);
                boolean isHeading2 = isHeading2(style, text);

                // 优先识别课时，避免 1.1 被误识别为第 1 章
                LessonMatch lessonMatch = tryMatchLesson(text, isHeading2);
                ChapterMatch chapterMatch = lessonMatch == null ? tryMatchChapter(text, isHeading1) : null;

                if (chapterMatch != null) {
                    // 保存前一个课时
                    flushLesson(result, currentChapterNo, currentChapterName, chapterDesc.toString(),
                            currentLessonNo, currentLessonName, lessonContent.toString());
                    currentLessonNo = null;
                    currentLessonName = null;
                    lessonContent.setLength(0);
                    lastChapterNo = chapterMatch.no != null ? chapterMatch.no : lastChapterNo + 1;
                    currentChapterNo = lastChapterNo;
                    currentChapterName = chapterMatch.name;
                    lastLessonNo = 0;
                    chapterDesc.setLength(0);
                } else if (lessonMatch != null) {
                    // 保存前一个课时
                    flushLesson(result, currentChapterNo, currentChapterName, chapterDesc.toString(),
                            currentLessonNo, currentLessonName, lessonContent.toString());
                    lastLessonNo = lessonMatch.no != null ? lessonMatch.no : lastLessonNo + 1;
                    currentLessonNo = lastLessonNo;
                    currentLessonName = lessonMatch.name;
                    lessonContent.setLength(0);
                } else {
                    // 普通正文：归属于课时或章节描述
                    if (currentLessonNo != null) {
                        lessonContent.append(text).append("\n");
                    } else if (currentChapterNo != null) {
                        chapterDesc.append(text).append("\n");
                    }
                }
            }
        }

        // 最后一个课时
        flushLesson(result, currentChapterNo, currentChapterName, chapterDesc.toString(),
                currentLessonNo, currentLessonName, lessonContent.toString());

        return result;
    }

    private void flushLesson(List<ChapterImportRowDTO> result, Integer chapterNo, String chapterName,
                             String chapterDesc, Integer lessonNo, String lessonName, String lessonContent) {
        if (chapterNo == null || !StringUtils.hasText(chapterName)) {
            return;
        }
        if (lessonNo == null || !StringUtils.hasText(lessonName)) {
            return;
        }
        ChapterImportRowDTO dto = new ChapterImportRowDTO();
        dto.setChapterNo(chapterNo);
        dto.setChapterName(chapterName);
        dto.setDescription(StringUtils.hasText(chapterDesc) ? chapterDesc.trim() : null);
        dto.setLessonNo(lessonNo);
        dto.setLessonName(lessonName);
        dto.setResourceType("video");
        dto.setContent(StringUtils.hasText(lessonContent) ? lessonContent.trim() : null);
        result.add(dto);
    }

    private boolean isHeading1(String style, String text) {
        if (style == null) {
            return false;
        }
        String lower = style.toLowerCase();
        return lower.contains("heading1") || lower.contains("标题 1") || lower.contains("标题1") || lower.equals("1");
    }

    private boolean isHeading2(String style, String text) {
        if (style == null) {
            return false;
        }
        String lower = style.toLowerCase();
        return lower.contains("heading2") || lower.contains("标题 2") || lower.contains("标题2") || lower.equals("2");
    }

    private ChapterMatch tryMatchChapter(String text, boolean forceHeading) {
        Matcher m = CHAPTER_PATTERN.matcher(text);
        if (m.matches()) {
            Integer no = null;
            if (m.group(1) != null) {
                no = Integer.parseInt(m.group(1));
            } else if (m.group(2) != null) {
                no = chineseToNumber(m.group(2));
            }
            String name = m.group(3).trim();
            if (no != null && StringUtils.hasText(name)) {
                return new ChapterMatch(no, name);
            }
        }
        // 数字章节：1. 名称 / 1、名称
        Matcher nm = NUMBER_CHAPTER_PATTERN.matcher(text);
        if (nm.matches()) {
            return new ChapterMatch(Integer.parseInt(nm.group(1)), nm.group(2).trim());
        }
        if (forceHeading) {
            // 标题样式但没有章节号：作为第 N 章
            return new ChapterMatch(null, text);
        }
        return null;
    }

    private LessonMatch tryMatchLesson(String text, boolean forceHeading) {
        Matcher m = LESSON_PATTERN.matcher(text);
        if (m.matches()) {
            int no;
            String name;
            if (m.group(1) != null) {
                // 第 1.2 节：课时序号为 2
                no = Integer.parseInt(m.group(2));
                name = m.group(5);
            } else {
                // 1.2：课时序号为 2
                no = Integer.parseInt(m.group(4));
                name = m.group(5);
            }
            if (StringUtils.hasText(name)) {
                return new LessonMatch(no, name.trim());
            }
        }
        // 课时 1 XXX
        Matcher lm = LESSON_NUMBER_PATTERN.matcher(text);
        if (lm.matches()) {
            return new LessonMatch(Integer.parseInt(lm.group(1)), lm.group(2).trim());
        }
        if (forceHeading) {
            return new LessonMatch(null, text);
        }
        return null;
    }

    private int chineseToNumber(String chinese) {
        Map<Character, Integer> map = new HashMap<>();
        map.put('一', 1); map.put('二', 2); map.put('三', 3); map.put('四', 4);
        map.put('五', 5); map.put('六', 6); map.put('七', 7); map.put('八', 8);
        map.put('九', 9); map.put('十', 10);
        int result = 0;
        int temp = 0;
        for (char c : chinese.toCharArray()) {
            Integer v = map.get(c);
            if (v == null) continue;
            if (v == 10) {
                if (temp == 0) temp = 1;
                result += temp * 10;
                temp = 0;
            } else {
                temp = temp * 10 + v;
            }
        }
        return result + temp;
    }

    private ColumnIndex parseHeader(XWPFTableRow headerRow) {
        ColumnIndex idx = new ColumnIndex();
        List<XWPFTableCell> cells = headerRow.getTableCells();
        for (int i = 0; i < cells.size(); i++) {
            String text = cells.get(i).getText().trim().toLowerCase();
            if (text.isEmpty()) continue;
            if (containsAny(text, "章节序号", "chapter_no", "chapterno")) idx.chapterNo = i;
            else if (containsAny(text, "章节名称", "chapter_name", "chaptername")) idx.chapterName = i;
            else if (containsAny(text, "课时序号", "lesson_no", "lessonno")) idx.lessonNo = i;
            else if (containsAny(text, "课时名称", "lesson_name", "lessonname")) idx.lessonName = i;
            else if (containsAny(text, "资源类型", "resource_type", "resourcetype")) idx.resourceType = i;
            else if (containsAny(text, "资源url", "resource_url", "resourceurl", "url", "视频链接", "链接")) idx.resourceUrl = i;
            else if (containsAny(text, "时长", "duration")) idx.duration = i;
            else if (containsAny(text, "内容", "content", "备注", "说明")) idx.content = i;
            else if (containsAny(text, "章节描述", "描述", "description")) idx.description = i;
        }
        return idx;
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) return true;
        }
        return false;
    }

    private ChapterImportRowDTO parseTableRow(XWPFTableRow row, ColumnIndex idx, int rowNum) {
        List<XWPFTableCell> cells = row.getTableCells();
        ChapterImportRowDTO dto = new ChapterImportRowDTO();
        dto.setChapterNo(getIntCell(cells, idx.chapterNo));
        dto.setChapterName(getStringCell(cells, idx.chapterName));
        dto.setDescription(getStringCell(cells, idx.description));
        dto.setLessonNo(getIntCell(cells, idx.lessonNo));
        dto.setLessonName(getStringCell(cells, idx.lessonName));
        dto.setResourceType(getStringCell(cells, idx.resourceType));
        dto.setResourceUrl(getStringCell(cells, idx.resourceUrl));
        dto.setDuration(getIntCell(cells, idx.duration));
        dto.setContent(getStringCell(cells, idx.content));

        if (isEmptyRow(dto)) {
            return null;
        }

        if (dto.getChapterNo() == null || dto.getChapterNo() <= 0) {
            failures.add(new ImportFailureDTO(rowNum, "章节序号必须大于0"));
            return null;
        }
        if (!StringUtils.hasText(dto.getChapterName())) {
            failures.add(new ImportFailureDTO(rowNum, "章节名称不能为空"));
            return null;
        }
        if (dto.getLessonNo() == null || dto.getLessonNo() <= 0) {
            failures.add(new ImportFailureDTO(rowNum, "课时序号必须大于0"));
            return null;
        }
        if (!StringUtils.hasText(dto.getLessonName())) {
            failures.add(new ImportFailureDTO(rowNum, "课时名称不能为空"));
            return null;
        }
        if (!StringUtils.hasText(dto.getResourceType())) {
            dto.setResourceType("video");
        }
        String rt = dto.getResourceType().trim().toLowerCase();
        if (!VALID_RESOURCE_TYPES.contains(rt)) {
            failures.add(new ImportFailureDTO(rowNum, "资源类型不合法：" + dto.getResourceType()));
            return null;
        }
        dto.setResourceType(rt);
        return dto;
    }

    private String getStringCell(List<XWPFTableCell> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        String text = cells.get(index).getText().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private Integer getIntCell(List<XWPFTableCell> cells, int index) {
        String text = getStringCell(cells, index);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isEmptyRow(ChapterImportRowDTO dto) {
        return dto.getChapterNo() == null
                && !StringUtils.hasText(dto.getChapterName())
                && dto.getLessonNo() == null
                && !StringUtils.hasText(dto.getLessonName());
    }

    private static class ColumnIndex {
        Integer chapterNo = -1;
        Integer chapterName = -1;
        Integer description = -1;
        Integer lessonNo = -1;
        Integer lessonName = -1;
        Integer resourceType = -1;
        Integer resourceUrl = -1;
        Integer duration = -1;
        Integer content = -1;

        boolean hasRequired() {
            return chapterNo >= 0 && chapterName >= 0 && lessonNo >= 0 && lessonName >= 0;
        }

        @Override
        public String toString() {
            return "ColumnIndex{" +
                    "chapterNo=" + chapterNo +
                    ", chapterName=" + chapterName +
                    ", lessonNo=" + lessonNo +
                    ", lessonName=" + lessonName +
                    ", resourceType=" + resourceType +
                    ", resourceUrl=" + resourceUrl +
                    ", duration=" + duration +
                    ", content=" + content +
                    ", description=" + description +
                    '}';
        }
    }

    private static class ChapterMatch {
        Integer no;
        String name;

        ChapterMatch(Integer no, String name) {
            this.no = no;
            this.name = name;
        }
    }

    private static class LessonMatch {
        Integer no;
        String name;

        LessonMatch(Integer no, String name) {
            this.no = no;
            this.name = name;
        }
    }
}
