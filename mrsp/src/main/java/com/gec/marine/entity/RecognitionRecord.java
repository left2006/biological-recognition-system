package com.gec.marine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("recognition_records")
public class RecognitionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String imageUrl;
    private String imageHash;
    private String recognitionResult;
    private Double confidenceScore;
    private Long speciesId;
    private Long processingTime;
    private Integer status;
    private String errorMessage;
    private Date createdTime;
    private String scientificName;
    private String commonName;
    private String conservationStatus;
    private String characteristics;
    private String habitat;
    private String distribution;
    private String sizeRange;
    private String diet;
    private String description;
    private String classification;
} 