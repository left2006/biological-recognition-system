package com.gec.marine.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 千问识别结果DTO
 */
@Data
@Accessors(chain = true)
public class QwenRecognitionDTO {

    /**
     * 学名
     */
    private String scientificName;

    /**
     * 俗名
     */
    private String commonName;

    /**
     * 中文名
     */
    private String chineseName;

    /**
     * 分类信息
     */
    private ClassificationInfo classification;

    /**
     * 栖息地描述
     */
    private String habitat;

    /**
     * 分布区域
     */
    private String distribution;

    /**
     * 特征描述
     */
    private String characteristics;

    /**
     * 体型范围
     */
    private String sizeRange;

    /**
     * 食性
     */
    private String diet;

    /**
     * 保护状态
     */
    private String conservationStatus;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 置信度
     */
    private Double confidence;

    @Data
    @Accessors(chain = true)
    public static class ClassificationInfo {
        private String kingdom;   // 界
        private String phylum;    // 门
        private String clazz;     // 纲
        private String order;     // 目
        private String family;    // 科
        private String genus;     // 属
        private String species;   // 种
    }
} 