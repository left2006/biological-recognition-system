package com.gec.marine.config;

import com.gec.marine.properties.QwenProperties;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG（Retrieval-Augmented Generation）系统核心配置类
 *
 * 作用概述：
 * 1. 配置和初始化通义千问大语言模型
 * 2. 配置向量嵌入模型用于文本向量化
 * 3. 配置向量存储组件用于相似性检索
 * 4. 提供RAG系统运行所需的核心组件Bean
 *
 * RAG工作流程：
 * 用户查询 -> 向量化(EmbeddingModel) -> 相似性检索(EmbeddingStore) ->
 * 检索到的上下文 + 用户问题 -> 大语言模型生成回答(ChatLanguageModel)
 *
 * @author Your Name
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class RagConfiguration {

    /**
     * 日志记录器，用于记录配置初始化过程和运行状态
     */
    private static final Logger logger = LoggerFactory.getLogger(RagConfiguration.class);

    /**
     * 通义千问相关配置属性
     * 包含API密钥、模型名称、温度参数、最大token数等配置信息
     */
    @Autowired
    private QwenProperties qwenProperties;


    /**
     * 配置文本嵌入模型Bean
     *
     * 作用：
     * - 将文本转换为高维向量表示
     * - 用于计算文本之间的语义相似性
     * - 支持用户查询的向量化和文档库的向量化
     *
     * 工作原理：
     * 1. 接收文本输入（用户问题或文档片段）
     * 2. 使用预训练的嵌入模型将文本转换为向量
     * 3. 向量可用于后续的相似性计算和检索
     *
     * @return EmbeddingModel 文本嵌入模型实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        logger.info("初始化通义千问嵌入模型: {}", qwenProperties.getEmbeddingModel());

        return QwenEmbeddingModel.builder()
                .apiKey(qwenProperties.getApiKey())                    // API访问密钥
                .modelName(qwenProperties.getEmbeddingModel())         // 嵌入模型名称
                .build();
    }

    /**
     * 配置向量存储Bean
     *
     * 作用：
     * - 存储文档片段及其对应的向量表示
     * - 提供基于向量相似性的快速检索功能
     * - 支持添加、删除、查询等向量操作
     *
     * 存储结构：
     * - TextSegment: 文本片段，包含原始文本和元数据
     * - Vector: 对应的向量表示
     * - 支持基于余弦相似度等算法进行相似性搜索
     *
     * 注意：InMemoryEmbeddingStore为内存存储，应用重启后数据丢失
     * 生产环境建议使用持久化存储如Pinecone、Weaviate等
     *
     * @return EmbeddingStore<TextSegment> 向量存储实例
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        logger.info("初始化内存向量存储");
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 系统初始化后的回调方法
     *
     * 作用：
     * - 在所有Bean初始化完成后执行
     * - 记录系统配置信息用于调试和监控
     * - 验证配置的正确性
     *
     * 安全考虑：
     * - API密钥只显示前10位字符，后续用***替代
     * - 避免在日志中完整暴露敏感信息
     */
    @PostConstruct
    public void initializeSystem() {
        logger.info("RAG系统配置初始化完成");

        // 安全地记录API密钥（只显示前10位）
        String maskedApiKey = qwenProperties.getApiKey().substring(0, 10) + "***";
        logger.info("API Key: {}", maskedApiKey);

        // 记录模型配置信息
        logger.info("Chat Model: {}", qwenProperties.getChatModel());
        logger.info("Embedding Model: {}", qwenProperties.getEmbeddingModel());
        logger.info("Temperature: {}", qwenProperties.getTemperature());
        logger.info("Max Tokens: {}", qwenProperties.getMaxTokens());

        // 系统状态确认
        logger.info("RAG系统各组件配置完成，可以接受查询请求");
    }
}