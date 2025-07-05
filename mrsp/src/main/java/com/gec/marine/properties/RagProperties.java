package com.gec.marine.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * RAG系统检索配置属性类
 *
 * 概述：
 * 该类专门管理RAG（Retrieval-Augmented Generation）系统中检索环节的核心参数，
 * 这些参数直接影响文档检索的质量、准确性和性能表现。
 *
 * 主要作用：
 * 1. 控制检索结果的数量和质量阈值
 * 2. 管理文档分块处理的粒度
 * 3. 优化检索性能和准确性的平衡
 * 4. 支持不同场景下的检索策略调整
 *
 * RAG检索流程：
 * 用户查询 → 查询向量化 → 相似性检索(maxResults条结果) →
 * 过滤低分结果(minScore阈值) → 构建上下文 → 生成回答
 *
 * 配置文件示例（application.yml）：
 * rag:
 *   max-results: 3
 *   min-score: 0.5
 *   chunk-size: 500
 *
 * @author Your Name
 * @version 1.0
 * @since 2024-01-01
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {


    private Integer maxResults = 3;
    private Double minScore = 0.5;
    private Integer chunkSize = 500;

    /**
     * 获取最大检索结果数量
     *
     * @return Integer 当前配置的最大结果数
     */
    public Integer getMaxResults() {
        return maxResults;
    }

    /**
     * 设置最大检索结果数量
     *
     * @param maxResults 最大结果数量，建议值：1-10
     */
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * 获取相似性得分最小阈值
     *
     * @return Double 当前配置的最小得分阈值
     */
    public Double getMinScore() {
        return minScore;
    }

    /**
     * 设置相似性得分最小阈值
     *
     * @param minScore 最小得分阈值，取值范围：0.0-1.0
     */
    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    /**
     * 获取文档分块大小
     *
     * @return Integer 当前配置的分块字符数
     */
    public Integer getChunkSize() {
        return chunkSize;
    }

    /**
     * 设置文档分块大小
     *
     * @param chunkSize 分块字符数，建议值：300-1000
     */
    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * 重写toString方法，便于调试和日志输出
     *
     * @return String 对象的字符串表示
     */
    @Override
    public String toString() {
        return "RagProperties{" +
                "maxResults=" + maxResults +
                ", minScore=" + minScore +
                ", chunkSize=" + chunkSize +
                '}';
    }

    /**
     * 验证配置参数的合理性
     * 用于在应用启动时检查配置的有效性
     *
     * @return boolean 配置是否有效
     */
    public boolean isValid() {
        return maxResults != null && maxResults > 0 &&
                minScore != null && minScore >= 0.0 && minScore <= 1.0 &&
                chunkSize != null && chunkSize > 0;
    }

    /**
     * 获取建议的重叠大小（分块大小的10%）
     * 用于文档分块时保持上下文连续性
     *
     * @return Integer 建议的重叠字符数
     */
    public Integer getRecommendedOverlapSize() {
        return Math.max(50, chunkSize / 10);
    }
}