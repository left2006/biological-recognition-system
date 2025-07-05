package com.gec.marine.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.marine.entity.RecognitionRecord;
import com.gec.marine.entity.PageResult;
import com.gec.marine.mapper.RecognitionRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class RecognitionRecordService {
    @Autowired
    private RecognitionRecordMapper recognitionRecordMapper;

    /**
     * 分页查询识别记录
     */
    public PageResult<RecognitionRecord> getPageRecords(Integer current, Integer size, String keyword, Long userId) {
        Page<RecognitionRecord> page = new Page<>(current, size);
        
        QueryWrapper<RecognitionRecord> queryWrapper = new QueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("recognition_result", keyword)
                       .or()
                       .like("image_url", keyword);
        }
        queryWrapper.orderByDesc("created_time");
        
        IPage<RecognitionRecord> pageResult = recognitionRecordMapper.selectPage(page, queryWrapper);
        
        return new PageResult<>(
            pageResult.getRecords(),
            pageResult.getTotal(),
            pageResult.getCurrent(),
            pageResult.getSize()
        );
    }

    /**
     * 获取所有识别记录（用于导出）
     */
    public List<RecognitionRecord> getAllRecords(String keyword, Long userId) {
        QueryWrapper<RecognitionRecord> queryWrapper = new QueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("recognition_result", keyword)
                       .or()
                       .like("image_url", keyword);
        }
        queryWrapper.orderByDesc("created_time");
        
        return recognitionRecordMapper.selectList(queryWrapper);
    }

    /**
     * 保存识别记录
     */
    public void save(RecognitionRecord record) {
        if (record.getRecognitionResult() == null) {
            record.setRecognitionResult("未知");
        }
        if (record.getConfidenceScore() == null) {
            record.setConfidenceScore(0.0);
        }
        if (record.getStatus() == null) {
            record.setStatus(1);
        }
        if (record.getCreatedTime() == null) {
            record.setCreatedTime(new java.util.Date());
        }
        recognitionRecordMapper.insert(record);
    }

    /**
     * 根据ID获取识别记录
     */
    public RecognitionRecord getById(Long id) {
        return recognitionRecordMapper.selectById(id);
    }

    /**
     * 删除识别记录
     */
    public void deleteById(Long id) {
        recognitionRecordMapper.deleteById(id);
    }

    /**
     * 批量删除识别记录
     */
    public void deleteByIds(List<Long> ids) {
        recognitionRecordMapper.deleteBatchIds(ids);
    }

    /**
     * 获取所有识别记录
     */
    public List<RecognitionRecord> listAll() {
        return recognitionRecordMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 根据用户ID获取识别记录
     */
    public List<RecognitionRecord> getByUserId(Long userId) {
        QueryWrapper<RecognitionRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("created_time");
        return recognitionRecordMapper.selectList(queryWrapper);
    }
} 