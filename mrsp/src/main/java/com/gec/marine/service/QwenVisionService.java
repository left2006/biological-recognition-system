package com.gec.marine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gec.marine.dto.QwenRecognitionDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里千问视觉识别服务
 */
@Service
@Slf4j
public class QwenVisionService {

    @Value("${qwen.api.key}")
    private String apiKey;

    @Value("${qwen.api.endpoint}")
    private String endpoint;

    @Value("${qwen.api.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 识别海洋生物图片
     */
    public QwenRecognitionDTO recognizeMarineSpecies(MultipartFile imageFile) throws IOException {
        // 将图片转换为Base64
        String base64Image = Base64.encodeBase64String(imageFile.getBytes());

        // 构建请求体
        Map<String, Object> requestBody = buildCorrectRequestBody(base64Image);

        // 发送请求到千问API
        String response = sendRequest(requestBody);

        // 解析响应并提取生物信息
        return parseResponse(response);
    }

    /**
     * 构建符合千问API格式的请求体
     */
    private Map<String, Object> buildCorrectRequestBody(String base64Image) {
        Map<String, Object> requestBody = new HashMap<>();

        // 设置模型
        requestBody.put("model", model != null ? model : "qwen-vl-plus");

        // 构建 input 部分
        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        // 添加文本内容
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("text", buildPrompt());
        content.add(textContent);

        // 添加图像内容
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("image", "data:image/jpeg;base64," + base64Image);
        content.add(imageContent);

        userMessage.put("content", content);
        messages.add(userMessage);

        input.put("messages", messages);
        requestBody.put("input", input);

        // 构建 parameters 部分
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", 2000);
        parameters.put("temperature", 0.1);
        parameters.put("top_p", 0.8);

        requestBody.put("parameters", parameters);

        return requestBody;
    }

    /**
     * 构建识别提示词
     */
    private String buildPrompt() {
        return """
        请仔细分析这张海洋生物图片，并以JSON格式返回详细信息。请严格按照以下JSON结构返回：
        {
            "scientificName": "学名",
            "commonName": "英文俗名", 
            "chineseName": "中文名",
            "classification": {
                "kingdom": "界",
                "phylum": "门",
                "clazz": "纲",
                "order": "目",
                "family": "科",
                "genus": "属",
                "species": "种"
            },
            "habitat": "栖息地描述",
            "distribution": "分布区域",
            "characteristics": "特征描述",
            "sizeRange": "体型范围",
            "diet": "食性",
            "conservationStatus": "保护状态",
            "description": "详细描述",
            "confidence": 0.95
        }
        
        如果无法确定具体种类，请标注"confidence"较低的值，并在相应字段中标注"未确定"或"疑似"。
        请只返回JSON数据，不要包含其他文字说明。
        """;
    }

    /**
     * 发送HTTP请求
     */
    private String sendRequest(Map<String, Object> requestBody) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(endpoint);

            // 设置请求头
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            // 序列化请求体
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, "UTF-8"));

            log.info("发送请求到千问API，请求体大小: {} bytes", jsonBody.length());

            // 发送请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, "UTF-8");

                log.info("千问API响应状态: {}", response.getStatusLine().getStatusCode());
                log.debug("千问API响应: {}", responseBody);

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("API请求失败: " + responseBody);
                }

                return responseBody;
            }
        }
    }

    /**
     * 解析API响应
     */
    private QwenRecognitionDTO parseResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String content = null;

            // 尝试按 DashScope Qwen-VL 格式提取 content.text 字段
            if (rootNode.has("output")) {
                JsonNode choicesNode = rootNode.path("output").path("choices");
                if (choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode firstChoice = choicesNode.get(0);
                    JsonNode contentArray = firstChoice.path("message").path("content");
                    if (contentArray.isArray() && contentArray.size() > 0) {
                        JsonNode textNode = contentArray.get(0).path("text");
                        if (textNode.isTextual()) {
                            content = textNode.asText();
                        }
                    }
                }
            }

            // OpenAI 风格（备用兼容）
            if (content == null && rootNode.has("choices")) {
                JsonNode choicesNode = rootNode.path("choices");
                if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                    JsonNode messageNode = choicesNode.get(0).path("message");
                    if (messageNode.has("content")) {
                        content = messageNode.path("content").asText();
                    }
                }
            }

            // 其他可能的格式
            if (content == null && rootNode.has("result")) {
                content = rootNode.path("result").path("response").asText();
            }

            if (content == null || content.trim().isEmpty()) {
                log.warn("无法找到响应内容，响应结构: {}", rootNode.toString());
                return createDefaultResponse();
            }

            log.info("千问返回内容长度: {}", content.length());
            log.debug("千问返回内容: {}", content);

            // 提取JSON部分
            String jsonContent = extractJsonFromContent(content);
            log.debug("提取的JSON: {}", jsonContent);

            // 解析JSON为DTO
            return objectMapper.readValue(jsonContent, QwenRecognitionDTO.class);

        } catch (Exception e) {
            log.error("解析千问响应失败: {}", e.getMessage(), e);
            return createDefaultResponse();
        }
    }

    /**
     * 从内容中提取JSON
     */
    private String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return createDefaultJsonResponse();
        }

        // 移除可能的markdown代码块标记
        content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

        // 查找JSON开始和结束位置
        int startIndex = content.indexOf("{");
        int endIndex = content.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String jsonContent = content.substring(startIndex, endIndex + 1);

            // 验证JSON格式
            try {
                objectMapper.readTree(jsonContent);
                return jsonContent;
            } catch (Exception e) {
                log.warn("提取的JSON格式无效: {}", e.getMessage());
            }
        }

        log.warn("无法从响应中提取有效JSON，原始内容: {}", content);
        return createDefaultJsonResponse();
    }

    /**
     * 创建默认响应
     */
    private QwenRecognitionDTO createDefaultResponse() {
        return new QwenRecognitionDTO()
                .setScientificName("未识别")
                .setCommonName("Unknown")
                .setChineseName("未知海洋生物")
                .setDescription("图片识别失败，请重新上传清晰的海洋生物图片")
                .setConfidence(0.0);
    }

    /**
     * 创建默认JSON响应
     */
    private String createDefaultJsonResponse() {
        return """
        {
            "scientificName": "未识别",
            "commonName": "Unknown",
            "chineseName": "未知海洋生物",
            "classification": {
                "kingdom": "未确定",
                "phylum": "未确定",
                "clazz": "未确定",
                "order": "未确定",
                "family": "未确定",
                "genus": "未确定",
                "species": "未确定"
            },
            "habitat": "未确定",
            "distribution": "未确定",
            "characteristics": "未确定",
            "sizeRange": "未确定",
            "diet": "未确定",
            "conservationStatus": "未确定",
            "description": "图片识别失败，请重新上传清晰的海洋生物图片",
            "confidence": 0.0
        }
        """;
    }
} 