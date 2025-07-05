package com.gec.marine.image;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImageRecognitionResult {
    private Long id;
    private String chineseName;
    private String scientificName;
    private String commonName;
    private String classification; // JSON格式的分类信息
    private String conservationStatus;
    private String characteristics;
    private String habitat;
    private String distribution;
    private String sizeRange;
    private String diet;
    private String description;
    private String imageUrls; // JSON数组格式的图片URL
    private Double confidence; // 识别置信度
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
} 