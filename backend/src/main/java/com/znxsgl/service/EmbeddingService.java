package com.znxsgl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地 BGE-M3 Embedding 服务客户端
 * 调用 Python 本地服务（默认 http://localhost:8000/embeddings）
 */
@Service
public class EmbeddingService {

    @Value("${embedding.local.url:http://localhost:8000/embeddings}")
    private String embeddingUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 单条文本向量化
     */
    public float[] embed(String text) {
        float[][] result = embedInternal(List.of(text));
        return result != null && result.length > 0 ? result[0] : null;
    }

    /**
     * 批量文本向量化，每批最多 25 条
     */
    public float[][] embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new float[0][];
        if (texts.size() <= 25) {
            return embedInternal(texts);
        }
        List<float[]> all = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += 25) {
            int end = Math.min(i + 25, texts.size());
            float[][] batch = embedInternal(texts.subList(i, end));
            if (batch != null) {
                for (float[] v : batch) all.add(v);
            } else {
                for (int j = i; j < end; j++) all.add(null);
            }
        }
        return all.toArray(new float[0][]);
    }

    private float[][] embedInternal(List<String> texts) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", "bge-m3");
            if (texts.size() == 1) {
                body.put("input", texts.get(0));
            } else {
                ArrayNode arr = body.putArray("input");
                texts.forEach(arr::add);
            }

            String reqJson = mapper.writeValueAsString(body);
            System.out.println("=== 本地 Embedding 请求: " + texts.size() + " 条文本 -> " + embeddingUrl);

            Request request = new Request.Builder()
                    .url(embeddingUrl)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(reqJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String bodyStr = response.body().string();
                if (response.code() != 200) {
                    System.out.println("=== 本地 Embedding 错误[" + response.code() + "]: "
                            + bodyStr.substring(0, Math.min(300, bodyStr.length())));
                    return null;
                }

                JsonNode node = mapper.readTree(bodyStr);
                JsonNode dataArr = node.path("data");
                if (!dataArr.isArray() || dataArr.size() == 0) {
                    System.out.println("=== 本地 Embedding 响应无 data: " + bodyStr.substring(0, 200));
                    return null;
                }

                float[][] result = new float[dataArr.size()][];
                for (int i = 0; i < dataArr.size(); i++) {
                    JsonNode emb = dataArr.get(i).path("embedding");
                    if (emb.isArray()) {
                        result[i] = new float[emb.size()];
                        for (int j = 0; j < emb.size(); j++) {
                            result[i][j] = (float) emb.get(j).asDouble();
                        }
                    }
                }
                System.out.println("=== 本地 Embedding 成功: " + result.length + " 条, 维度="
                        + (result[0] != null ? result[0].length : "?"));
                return result;
            }
        } catch (Exception e) {
            System.out.println("=== 本地 Embedding 异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
