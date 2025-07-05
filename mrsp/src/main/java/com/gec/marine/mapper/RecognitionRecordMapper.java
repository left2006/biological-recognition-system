package com.gec.marine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.marine.entity.RecognitionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RecognitionRecordMapper extends BaseMapper<RecognitionRecord> {
    
    /**
     * 根据用户ID查询识别记录
     */
    @Select("SELECT * FROM recognition_records WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<RecognitionRecord> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据关键词搜索识别记录
     */
    @Select("SELECT * FROM recognition_records WHERE recognition_result LIKE CONCAT('%', #{keyword}, '%') OR image_url LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_time DESC")
    List<RecognitionRecord> selectByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计用户识别记录数量
     */
    @Select("SELECT COUNT(*) FROM recognition_records WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 获取识别成功率统计
     */
    @Select("SELECT status, COUNT(*) as count FROM recognition_records GROUP BY status")
    List<Object> getStatusStatistics();
} 