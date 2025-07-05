package com.gec.marine.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 通义千问API配置属性类
 *
 * 概述：
 * 该类用于管理deepseek大语言模型的各项配置参数，通过Spring Boot的
 * @ConfigurationProperties机制从配置文件中自动绑定配置值。
 *
 * 主要作用：
 * 1. 统一管理deepseeAPI的配置参数
 * 2. 提供配置的默认值和类型安全保障
 * 3. 支持外部化配置，便于不同环境的配置管理
 * 4. 实现配置的集中化管理和维护
 *
 * 配置文件示例（application.yml）：
 * qwen:
 *   api-key: "your-api-key-here"
 *   chat-model: "qwen-plus"
 *   embedding-model: "text-embedding-v1"
 *   temperature: 0.3
 *   max-tokens: 2000
 *
 * @author Your Name
 * @version 1.0
 * @since 2024-01-01
 */
@Component
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    /**
     * API相关配置
     */
    private Api api = new Api();

    /**
     * 聊天对话模型名称
     *
     * 可选模型：
     * - qwen-turbo: 超快响应，适合简单对话场景
     * - qwen-plus: 平衡性能和效果，推荐日常使用（默认）
     * - qwen-max: 顶级效果，适合复杂推理任务
     * - qwen-max-longcontext: 支持长文本上下文的高级模型
     *
     * 选择建议：
     * - 简单问答：qwen-turbo
     * - 通用场景：qwen-plus
     * - 复杂任务：qwen-max
     * - 长文档处理：qwen-max-longcontext
     */
    private String chatModel = "qwen-plus";

    /**
     * 文本嵌入向量模型名称
     *
     * 可选模型：
     * - text-embedding-v1: 通用文本嵌入模型（默认）
     * - text-embedding-v2: 升级版嵌入模型，效果更佳
     *
     * 作用：
     * - 将文本转换为高维度向量表示
     * - 用于计算文本之间的语义相似性
     * - 支持多语言文本的向量化处理
     *
     * 应用场景：
     * - 文档检索和相似性匹配
     * - 语义搜索和推荐系统
     * - 文本聚类和分类任务
     */
    private String embeddingModel = "text-embedding-v1";

    /**
     * 生成文本的随机性控制参数（温度值）
     *
     * 取值范围：0.0 - 1.0
     *
     * 参数说明：
     * - 0.0-0.3: 保守生成，输出更确定和一致
     * - 0.3-0.7: 平衡创造性和一致性（推荐范围）
     * - 0.7-1.0: 高创造性，输出更多样化但可能不够稳定
     *
     * 应用指导：
     * - 事实性回答：使用较低温度（0.1-0.3）
     * - 创意写作：使用中等温度（0.5-0.7）
     * - 头脑风暴：使用较高温度（0.7-0.9）
     *
     * 默认值：0.3（适合大多数问答场景）
     */
    private Float temperature = 0.3f;

    /**
     * 单次生成的最大Token数量限制
     *
     * Token说明：
     * - Token是文本处理的基本单位，大致相当于一个词或字符
     * - 中文：1个汉字 ≈ 1-2个Token
     * - 英文：1个单词 ≈ 1-2个Token
     *
     * 取值建议：
     * - 简短回答：500-1000
     * - 常规对话：1000-2000（默认）
     * - 长文本生成：2000-4000
     * - 文章写作：4000-8000
     *
     * 注意事项：
     * - 更大的Token数会消耗更多API调用费用
     * - 某些模型对最大Token数有上限限制
     * - 建议根据实际需求合理设置
     */
    private Integer maxTokens = 2000;

    /**
     * API配置内部类
     */
    public static class Api {
        private String key;
        private String endpoint;
        private String model;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    /**
     * 获取API密钥（兼容旧版本）
     */
    public String getApiKey() {
        return api != null ? api.getKey() : null;
    }

    /**
     * 设置API密钥（兼容旧版本）
     */
    public void setApiKey(String apiKey) {
        if (api == null) {
            api = new Api();
        }
        api.setKey(apiKey);
    }

    /**
     * 获取聊天模型名称
     *
     * @return String 当前配置的聊天模型名称
     */
    public String getChatModel() {
        return chatModel;
    }

    /**
     * 设置聊天模型名称
     *
     * @param chatModel 聊天模型名称
     */
    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 获取嵌入模型名称
     *
     * @return String 当前配置的嵌入模型名称
     */
    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * 设置嵌入模型名称
     *
     * @param embeddingModel 嵌入模型名称
     */
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 获取温度参数值
     *
     * @return Float 当前配置的温度值
     */
    public Float getTemperature() {
        return temperature;
    }

    /**
     * 设置温度参数值
     *
     * @param temperature 温度值（0.0-1.0）
     */
    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    /**
     * 获取最大Token数限制
     *
     * @return Integer 当前配置的最大Token数
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * 设置最大Token数限制
     *
     * @param maxTokens 最大Token数
     */
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * 重写toString方法，用于调试和日志输出
     * 注意：为了安全考虑，API密钥进行了脱敏处理
     *
     * @return String 对象的字符串表示
     */
    @Override
    public String toString() {
        return "QwenProperties{" +
                "api=" + api +
                ", chatModel='" + chatModel + '\'' +
                ", embeddingModel='" + embeddingModel + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                '}';
    }
}