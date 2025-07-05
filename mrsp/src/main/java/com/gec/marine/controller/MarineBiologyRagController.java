package com.gec.marine.controller;

import com.gec.marine.entity.Result;
import com.gec.marine.service.MarineBiologyRagService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 海洋生物RAG智能客服系统的RESTful API控制器
 *
 * 【控制器概述】
 * 这是系统的Web API入口层，负责：
 * 1. 接收和处理来自前端、移动端、第三方系统的HTTP请求
 * 2. 参数验证和格式化处理
 * 3. 调用业务服务层(FlightRagService)处理具体逻辑
 * 4. 统一封装响应格式并返回JSON数据
 * 5. 异常处理和错误信息标准化
 *
 * 【API设计原则】
 * - RESTful风格：使用标准HTTP方法和状态码
 * - 统一响应格式：所有接口返回结构化的JSON响应
 * - 向后兼容：保留旧版本接口以支持现有客户端
 * - 跨域支持：允许前端跨域访问
 * - 完整错误处理：提供详细的错误信息和日志记录
 *
 * 【支持的客户端】
 * - Web前端(React/Vue等SPA应用)
 * - 移动应用(iOS/Android)
 * - 第三方集成系统
 * - API测试工具(Postman等)
 *
 * @author System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController                                    // 标识为REST控制器，自动进行JSON序列化
@RequestMapping("/marineBiology")                         // 统一路由前缀，所有接口都以/flight开头
public class MarineBiologyRagController {

    // 日志记录器，用于记录请求处理过程和错误信息
    private static final Logger logger = LoggerFactory.getLogger(MarineBiologyRagController.class);

    /**
     * 注入MarineBiologyRagService业务服务
     * 通过依赖注入获取RAG服务实例，实现控制器与业务逻辑的解耦
     */
    @Autowired
    private MarineBiologyRagService marineBiologyRagService;

    // 或者，如果您希望始终返回 HTTP 200 状态码，可以这样写：
    @PostMapping("/query")
    public Result<Map<String, Object>> query(
            @RequestBody String query) {
        logger.info("收到查询请求: {}", query);
        try {
            // 调用RAG服务处理查询，trim()去除首尾空格
            String response = marineBiologyRagService.processQuery(query.trim());

            // 构建查询结果数据
            Map<String, Object> queryData = new HashMap<>();
            queryData.put("timestamp", LocalDateTime.now());   // 处理时间戳
            queryData.put("query", query);                     // 原始查询内容
            queryData.put("response", response);               // AI生成的回答
            queryData.put("message", "查询成功");               // 处理状态描述

            // 返回成功结果
            return Result.ok(queryData);

        } catch (Exception e) {
            // 记录错误日志，包含完整的异常堆栈信息
            logger.error("处理查询请求失败: {}", e.getMessage(), e);

            // 返回失败结果
            String errorMessage = "系统处理异常，请稍后重试: " + e.getMessage();
            return Result.failed(errorMessage);
        }
    }

    // 1. 修改Controller方法，添加正确的编码设置
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam String message, HttpServletResponse response) {
        // 设置响应头确保UTF-8编码
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        return marineBiologyRagService.processQueryStream(message);
    }


}