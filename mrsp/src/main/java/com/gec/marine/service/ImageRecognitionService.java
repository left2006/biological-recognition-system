package com.gec.marine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gec.marine.dto.QwenRecognitionDTO;
import com.gec.marine.image.ImageRecognitionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class ImageRecognitionService {
    
    @Autowired
    private QwenVisionService qwenVisionService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 简单的内存存储，实际项目中应该使用数据库
    private final Map<Long, ImageRecognitionResult> resultCache = new ConcurrentHashMap<>();
    private long idCounter = 1;

    /**
     * 识别海洋生物图片
     */
    public ImageRecognitionResult recognize(MultipartFile imageFile) {
        String imageUrl = null;
        String imageUrlWithPrefix = null;
        try {
            // 保存图片到本地
            String uploadDir = "uploads/images/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String originalFilename = imageFile.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            String filePath = uploadDir + fileName;
            Path path = Paths.get(filePath);
            Files.write(path, imageFile.getBytes());
            // 构造可访问URL
            imageUrl = "/uploads/images/" + fileName;
            imageUrlWithPrefix = "/mrsp_server" + imageUrl;

            // 调用千问API进行识别
            QwenRecognitionDTO qwenResult = qwenVisionService.recognizeMarineSpecies(imageFile);
            
            // 转换为ImageRecognitionResult
            ImageRecognitionResult result = convertToImageRecognitionResult(qwenResult);
            result.setId(idCounter++);
            result.setCreatedTime(LocalDateTime.now());
            result.setUpdatedTime(LocalDateTime.now());
            // 设置图片URL
            result.setImageUrls("[\"" + imageUrlWithPrefix + "\"]");
            // 缓存结果
            resultCache.put(result.getId(), result);
            log.info("图像识别成功，ID: {}, 物种: {}", result.getId(), result.getChineseName());
            return result;
        } catch (Exception e) {
            log.error("图像识别失败: {}", e.getMessage(), e);
            QwenRecognitionDTO qwenResult = new QwenRecognitionDTO();
            return createDefaultResult(qwenResult, imageUrlWithPrefix);
        }
    }

    /**
     * 根据ID获取识别结果
     */
    public ImageRecognitionResult getResultById(Long id) {
        ImageRecognitionResult result = resultCache.get(id);
        if (result == null) {
            log.warn("未找到ID为{}的识别结果", id);
            QwenRecognitionDTO qwenResult = new QwenRecognitionDTO();
            return createDefaultResult(qwenResult, null);
        }
        return result;
    }

    /**
     * 将QwenRecognitionDTO转换为ImageRecognitionResult
     */
    private ImageRecognitionResult convertToImageRecognitionResult(QwenRecognitionDTO qwenResult) {
        ImageRecognitionResult result = new ImageRecognitionResult();
        result.setChineseName(qwenResult.getChineseName());
        result.setScientificName(qwenResult.getScientificName());
        result.setCommonName(qwenResult.getCommonName());
        result.setConservationStatus(qwenResult.getConservationStatus());
        result.setCharacteristics(qwenResult.getCharacteristics());
        result.setHabitat(qwenResult.getHabitat());
        result.setDistribution(qwenResult.getDistribution());
        result.setSizeRange(qwenResult.getSizeRange());
        result.setDiet(qwenResult.getDiet());
        result.setDescription(qwenResult.getDescription());
        result.setConfidence(qwenResult.getConfidence());
        // 转换分类信息为JSON字符串
        if (qwenResult.getClassification() != null) {
            try {
                result.setClassification(objectMapper.writeValueAsString(qwenResult.getClassification()));
            } catch (Exception e) {
                log.warn("转换分类信息失败: {}", e.getMessage());
                result.setClassification("{}");
            }
        }
        return result;
    }

    /**
     * 创建默认识别结果
     */
    private ImageRecognitionResult createDefaultResult(QwenRecognitionDTO qwenResult, String imageUrlWithPrefix) {
        ImageRecognitionResult result = new ImageRecognitionResult();
        // 优先保留千问API原始内容，只在完全无内容时兜底
        result.setChineseName(qwenResult.getChineseName() != null && !qwenResult.getChineseName().isEmpty() ? qwenResult.getChineseName() : "未知物种");
        result.setScientificName(qwenResult.getScientificName() != null && !qwenResult.getScientificName().isEmpty() ? qwenResult.getScientificName() : "未知");
        result.setCommonName(qwenResult.getCommonName() != null && !qwenResult.getCommonName().isEmpty() ? qwenResult.getCommonName() : "Unknown");
        result.setConservationStatus(qwenResult.getConservationStatus() != null && !qwenResult.getConservationStatus().isEmpty() ? qwenResult.getConservationStatus() : "未知");
        result.setCharacteristics(qwenResult.getCharacteristics() != null && !qwenResult.getCharacteristics().isEmpty() ? qwenResult.getCharacteristics() : "");
        result.setHabitat(qwenResult.getHabitat() != null && !qwenResult.getHabitat().isEmpty() ? qwenResult.getHabitat() : "");
        result.setDistribution(qwenResult.getDistribution() != null && !qwenResult.getDistribution().isEmpty() ? qwenResult.getDistribution() : "");
        result.setSizeRange(qwenResult.getSizeRange() != null && !qwenResult.getSizeRange().isEmpty() ? qwenResult.getSizeRange() : "");
        result.setDiet(qwenResult.getDiet() != null && !qwenResult.getDiet().isEmpty() ? qwenResult.getDiet() : "");
        result.setDescription(qwenResult.getDescription() != null && !qwenResult.getDescription().isEmpty() ? qwenResult.getDescription() : "未能识别该物种，建议上传更清晰的图片或更换角度。");
        result.setConfidence(qwenResult.getConfidence() != null ? qwenResult.getConfidence() : 0.0);
        result.setClassification("{}");
        result.setImageUrls(imageUrlWithPrefix != null ? "[\"" + imageUrlWithPrefix + "\"]" : "[]");
        return result;
    }
} 