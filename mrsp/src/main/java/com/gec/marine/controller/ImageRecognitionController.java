package com.gec.marine.controller;

import com.gec.marine.entity.Result;
import com.gec.marine.entity.PageResult;
import com.gec.marine.entity.RecognitionRecord;
import com.gec.marine.image.ImageRecognitionResult;
import com.gec.marine.service.ImageRecognitionService;
import com.gec.marine.service.RecognitionRecordService;
import com.gec.marine.service.QwenVisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.gec.marine.dto.QwenRecognitionDTO;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RestController
@RequestMapping("/image-recognition")
@Slf4j
public class ImageRecognitionController {
    
    @Autowired
    private ImageRecognitionService imageRecognitionService;
    
    @Autowired
    private RecognitionRecordService recognitionRecordService;

    @Autowired
    private QwenVisionService qwenVisionService;

    /**
     * 上传图片并识别
     */
    @PostMapping("/upload")
    public Result<ImageRecognitionResult> uploadAndRecognize(@RequestParam("imageFile") MultipartFile imageFile,
                                                             @RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.failed("缺少用户ID，请重新登录");
        }
        try {
            if (imageFile.isEmpty()) {
                return Result.failed("请选择要上传的图片");
            }
            
            // 检查文件类型
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.failed("请上传图片文件");
            }
            
            // 检查文件大小（10MB）
            if (imageFile.getSize() > 10 * 1024 * 1024) {
                return Result.failed("图片文件大小不能超过10MB");
            }
            
            log.info("开始识别图片: {}, 大小: {} bytes", imageFile.getOriginalFilename(), imageFile.getSize());
            
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
            String imageUrl = "/uploads/images/" + fileName;
            String imageUrlWithPrefix = "/mrsp_server" + imageUrl;

            // 调用识别
            ImageRecognitionResult result = imageRecognitionService.recognize(imageFile);

            // 保存识别记录到数据库
            RecognitionRecord record = new RecognitionRecord();
            record.setUserId(userId);
            record.setImageUrl(imageUrlWithPrefix);
            record.setRecognitionResult(result.getChineseName() != null ? result.getChineseName() : "未知");
            record.setConfidenceScore(result.getConfidence() != null ? result.getConfidence() : 0.0);
            record.setStatus(1);
            record.setCreatedTime(new java.util.Date());
            record.setScientificName(result.getScientificName());
            record.setCommonName(result.getCommonName());
            record.setConservationStatus(result.getConservationStatus());
            record.setCharacteristics(result.getCharacteristics());
            record.setHabitat(result.getHabitat());
            record.setDistribution(result.getDistribution());
            record.setSizeRange(result.getSizeRange());
            record.setDiet(result.getDiet());
            record.setDescription(result.getDescription());
            record.setClassification(result.getClassification());
            recognitionRecordService.save(record);

            // 返回时也把图片URL写入result
            result.setImageUrls("[\"" + imageUrlWithPrefix + "\"]");

            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("图片识别失败: {}", e.getMessage(), e);
            return Result.failed("图片识别失败: " + e.getMessage());
        }
    }

    /**
     * 识别记录详情
     */
    @GetMapping("/detail/{id}")
    public Result<ImageRecognitionResult> getDetail(@PathVariable("id") Long id) {
        try {
            if (id == null || id <= 0) {
                return Result.failed("无效的ID参数");
            }
            // 优先查数据库
            RecognitionRecord record = recognitionRecordService.getById(id);
            if (record != null) {
                ImageRecognitionResult result = new ImageRecognitionResult();
                result.setId(record.getId());
                result.setChineseName(record.getRecognitionResult());
                result.setConfidence(record.getConfidenceScore());
                result.setImageUrls("[\"" + record.getImageUrl() + "\"]");
                result.setScientificName(record.getScientificName());
                result.setCommonName(record.getCommonName());
                result.setConservationStatus(record.getConservationStatus());
                result.setCharacteristics(record.getCharacteristics());
                result.setHabitat(record.getHabitat());
                result.setDistribution(record.getDistribution());
                result.setSizeRange(record.getSizeRange());
                result.setDiet(record.getDiet());
                result.setDescription(record.getDescription());
                result.setClassification(record.getClassification());
                return Result.ok(result);
            }
            // 若数据库无，再查缓存
            ImageRecognitionResult result = imageRecognitionService.getResultById(id);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取识别详情失败: {}", e.getMessage(), e);
            return Result.failed("获取识别详情失败: " + e.getMessage());
        }
    }

    /**
     * 分页获取识别记录
     */
    @GetMapping("/records")
    public Result<PageResult<RecognitionRecord>> getRecords(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.failed("缺少用户ID，请重新登录");
        }
        try {
            PageResult<RecognitionRecord> pageResult = recognitionRecordService.getPageRecords(current, size, keyword, userId);
            return Result.ok(pageResult);
        } catch (Exception e) {
            log.error("获取识别记录失败: {}", e.getMessage(), e);
            return Result.failed("获取识别记录失败: " + e.getMessage());
        }
    }

    /**
     * 导出识别记录
     */
    @GetMapping("/records/export")
    public void exportRecords(HttpServletResponse response,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null) {
            try {
                response.setStatus(400);
                response.getWriter().write("缺少用户ID，请重新登录");
            } catch (IOException ex) {}
            return;
        }
        try {
            List<RecognitionRecord> records = recognitionRecordService.getAllRecords(keyword, userId);
            
            // 设置响应头
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", 
                "attachment; filename=recognition_records_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            
            // 写入CSV内容
            PrintWriter writer = response.getWriter();
            writer.write("\uFEFF"); // BOM for UTF-8
            
            // 写入表头
            writer.println("ID,用户ID,图片URL,识别结果,置信度,状态,创建时间");
            
            // 写入数据
            for (RecognitionRecord record : records) {
                writer.println(String.format("%d,%d,%s,%s,%.2f,%d,%s",
                    record.getId(),
                    record.getUserId(),
                    record.getImageUrl(),
                    record.getRecognitionResult(),
                    record.getConfidenceScore(),
                    record.getStatus(),
                    record.getCreatedTime()
                ));
            }
            
            writer.flush();
            writer.close();
            
        } catch (Exception e) {
            log.error("导出识别记录失败: {}", e.getMessage(), e);
            try {
                response.setStatus(500);
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    /**
     * 批量图片识别
     */
    @PostMapping("/batch-upload")
    public Result<List<Map<String, Object>>> batchUpload(@RequestParam("imageFiles") MultipartFile[] imageFiles,
                                                        @RequestParam(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.failed("缺少用户ID，请重新登录");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            for (MultipartFile imageFile : imageFiles) {
                if (imageFile.isEmpty()) continue;
                ImageRecognitionResult result = imageRecognitionService.recognize(imageFile);
                RecognitionRecord record = new RecognitionRecord();
                record.setUserId(userId);
                record.setImageUrl(result.getImageUrls().replace("[\"", "").replace("\"]", ""));
                record.setRecognitionResult(result.getChineseName() != null ? result.getChineseName() : "未知");
                record.setConfidenceScore(result.getConfidence() != null ? result.getConfidence() : 0.0);
                record.setStatus(1);
                record.setCreatedTime(new java.util.Date());
                record.setScientificName(result.getScientificName());
                record.setCommonName(result.getCommonName());
                record.setConservationStatus(result.getConservationStatus());
                record.setCharacteristics(result.getCharacteristics());
                record.setHabitat(result.getHabitat());
                record.setDistribution(result.getDistribution());
                record.setSizeRange(result.getSizeRange());
                record.setDiet(result.getDiet());
                record.setDescription(result.getDescription());
                record.setClassification(result.getClassification());
                recognitionRecordService.save(record);
                // 重新从数据库查，确保字段和ID一致
                RecognitionRecord dbRecord = recognitionRecordService.getById(record.getId());
                Map<String, Object> map = new HashMap<>();
                map.put("recordId", dbRecord.getId());
                map.put("imageUrl", dbRecord.getImageUrl());
                map.put("chineseName", dbRecord.getRecognitionResult());
                map.put("confidence", dbRecord.getConfidenceScore());
                map.put("status", "成功");
                map.put("scientificName", dbRecord.getScientificName());
                map.put("commonName", dbRecord.getCommonName());
                map.put("conservationStatus", dbRecord.getConservationStatus());
                map.put("characteristics", dbRecord.getCharacteristics());
                map.put("habitat", dbRecord.getHabitat());
                map.put("distribution", dbRecord.getDistribution());
                map.put("sizeRange", dbRecord.getSizeRange());
                map.put("diet", dbRecord.getDiet());
                map.put("description", dbRecord.getDescription());
                map.put("classification", dbRecord.getClassification());
                results.add(map);
            }
            return Result.ok(results);
        } catch (Exception e) {
            log.error("批量识别图片失败: {}", e.getMessage(), e);
            return Result.failed("批量识别图片失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("图像识别服务运行正常");
    }

    /**
     * 删除识别记录
     */
    @PostMapping("/delete/{id}")
    public Result<String> deleteRecord(@PathVariable("id") Long id) {
        try {
            recognitionRecordService.deleteById(id);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除识别记录失败: {}", e.getMessage(), e);
            return Result.failed("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除识别记录
     */
    @PostMapping("/delete-batch")
    public Result<String> deleteRecords(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.failed("请选择要删除的记录");
            }
            recognitionRecordService.deleteByIds(ids);
            return Result.ok("批量删除成功，共删除 " + ids.size() + " 条记录");
        } catch (Exception e) {
            e.printStackTrace(); // 输出详细异常堆栈
            log.error("批量删除识别记录失败: {}", e.getMessage(), e);
            return Result.failed("批量删除失败: " + e.getMessage());
        }
    }

    // 工具方法：将本地图片文件转为MultipartFile（简单实现）
    private MultipartFile fileToMultipartFile(File file) throws IOException {
        byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
        return new MultipartFile() {
            @Override
            public String getName() { return file.getName(); }
            @Override
            public String getOriginalFilename() { return file.getName(); }
            @Override
            public String getContentType() { return "image/jpeg"; }
            @Override
            public boolean isEmpty() { return content.length == 0; }
            @Override
            public long getSize() { return content.length; }
            @Override
            public byte[] getBytes() throws IOException { return content; }
            @Override
            public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }
            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException { java.nio.file.Files.write(dest.toPath(), content); }
        };
    }
} 