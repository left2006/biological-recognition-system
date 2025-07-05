package com.gec.marine.service;

import com.gec.marine.properties.RagProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class MarineBiologyRagService {

    private static final Logger logger = LoggerFactory.getLogger(MarineBiologyRagService.class);

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${deepseek.timeout:60}")
    private Integer timeoutSeconds;

    @Value("${deepseek.max-tokens:4000}")
    private Integer maxTokens;

    @Value("${deepseek.temperature:0.3}")
    private Double temperature;

    // 海洋生物专家助手接口定义
    public interface MarineBiologyExpert {
        String chat(String message);
        TokenStream stream(String message);
    }

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;
    private MarineBiologyExpert marineBiologyExpert;
    private ContentRetriever contentRetriever; // 内容检索器

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private RagProperties ragProperties;

    private boolean isInitialized = false;

    /**
     * 初始化DeepSeek聊天模型和海洋生物专家助手
     */
    @PostConstruct
    public void initializeModels() {
        try {
            logger.info("初始化DeepSeek聊天模型（标准+流式）...");

            if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty() || deepseekApiKey.equals("your-deepseek-api-key-here")) {
                throw new IllegalStateException("DeepSeek API Key未配置或配置错误");
            }

            // 初始化标准聊天模型
            this.chatModel = OpenAiChatModel.builder()
                    .baseUrl(deepseekBaseUrl)
                    .apiKey(deepseekApiKey)
                    .modelName(deepseekModel)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            // 初始化流式聊天模型
            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .baseUrl(deepseekBaseUrl)
                    .apiKey(deepseekApiKey)
                    .modelName(deepseekModel)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            // 初始化海洋生物知识库
            initializeMarineKnowledgeBase();

            // 创建海洋生物专家助手实例
            initializeMarineBiologyExpert();

            logger.info("DeepSeek模型和海洋生物专家助手初始化完成（支持标准和流式响应）");

        } catch (IllegalStateException e) {
            logger.error("DeepSeek配置错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("DeepSeek模型初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("DeepSeek模型初始化失败", e);
        }
    }

    /**
     * 初始化海洋生物专家助手 - 集成RAG和聊天记忆
     */
    private void initializeMarineBiologyExpert() {
        try {
            // 1. 创建持久化聊天记忆
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

            // 2. 创建内容检索器 - 这是关键步骤
            this.contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(ragProperties.getMaxResults())
                    .minScore(ragProperties.getMinScore())
                    .build();

            // 3. 构建海洋生物专家助手，集成RAG功能
            this.marineBiologyExpert = AiServices.builder(MarineBiologyExpert.class)
                    // 设置标准聊天模型
                    .chatLanguageModel(chatModel)
                    // 设置流式聊天模型
                    .streamingChatLanguageModel(streamingChatModel)
                    // 配置聊天记忆
                    .chatMemory(chatMemory)
                    // 关键：添加内容检索器，实现RAG功能
                    .contentRetriever(contentRetriever)
                    .systemMessageProvider(s->buildSystemMessageString())
                    .build();

            logger.info("海洋生物专家助手（含RAG功能）初始化完成");
        } catch (Exception e) {
            logger.error("海洋生物专家助手初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("海洋生物专家助手初始化失败", e);
        }
    }

    /**
     * 构建海洋生物专家系统消息
     */
    private String buildSystemMessageString() {
        return """
        你是一位专业的海洋生物学专家，拥有丰富的海洋生物知识和研究经验，使用DeepSeek AI技术为用户提供准确的海洋生物相关咨询。

        ## 专业领域：
        1. **海洋生物分类**：鱼类、哺乳动物、无脊椎动物、海洋植物等
        2. **生态系统**：珊瑚礁、深海、海岸带、极地海洋等
        3. **生物行为**：觅食、繁殖、迁徙、社会行为等
        4. **保护现状**：濒危物种、保护措施、环境威胁等
        5. **科学研究**：最新发现、研究方法、观察技术等

        ## 回答规范：
        1. **科学准确性**：严格基于提供的参考信息和科学事实回答
        2. **专业性**：使用准确的科学术语，同时确保通俗易懂
        3. **详细性**：提供具体的数据、特征、分布等信息
        4. **结构化**：回答要有条理，适当使用分类和列举
        5. **教育性**：帮助用户了解海洋生物的奇妙世界
        6. **上下文感知**：记住之前的对话内容，提供连贯的专业指导
        
        ## 重要提醒：
        - 当用户提问时，会自动从海洋生物知识库检索相关信息
        - 优先使用检索到的准确科学信息回答
        - 如果检索信息不完整，明确说明知识局限性
        - 保持对话连贯性，记住用户之前提到的海洋生物话题
        - 鼓励海洋保护意识和科学探索精神
        
        请以专业、友好的方式回答用户关于海洋生物的问题，分享海洋世界的精彩知识。
        """;
    }

    /**
     * 处理海洋生物相关查询（RAG自动集成）
     */
    public String processQuery(String userQuery) {
        if (!isInitialized) {
            return "海洋生物知识库正在初始化中，请稍后再试...";
        }

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "请告诉我您想了解的海洋生物问题，我会为您提供专业的解答。";
        }

        try {
            logger.info("处理海洋生物查询: {}", userQuery);

            // 直接使用海洋生物专家助手（已集成RAG功能）
            String response = marineBiologyExpert.chat(userQuery);

            System.out.println(response);

            logger.info("成功处理查询，返回回答长度: {} 字符", response.length());
            return formatFinalResponse(response, userQuery);

        } catch (Exception e) {
            logger.error("处理查询时发生错误: {}", e.getMessage(), e);
            return "抱歉，系统暂时无法处理您的海洋生物问题。请稍后再试或参考海洋生物百科全书。";
        }
    }

    /**
     * 流式查询处理（RAG自动集成）
     */
    public Flux<String> processQueryStream(String userQuery) {
        if (!isInitialized) {
            return Flux.just("海洋生物知识库正在初始化中，请稍后再试...");
        }

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return Flux.just("请告诉我您想了解的海洋生物问题，我会为您提供专业的解答。");
        }

        try {
            logger.info("开始流式处理海洋生物查询: {}", userQuery);

            // 直接使用海洋生物专家助手的stream方法（已集成RAG）
            TokenStream stream = marineBiologyExpert.stream(userQuery);

            return Flux.create(sink -> {
                sink.next("🐋 海洋生物专家回复：\n\n");

                stream
                        .onPartialResponse(partialResponse -> {
                            if (partialResponse != null && !partialResponse.trim().isEmpty()) {
                                try {
                                    String encodedResponse = new String(
                                            partialResponse.getBytes(StandardCharsets.UTF_8),
                                            StandardCharsets.UTF_8
                                    );
                                    sink.next(encodedResponse);
                                } catch (Exception e) {
                                    logger.warn("编码响应失败，使用原始内容: {}", e.getMessage());
                                    sink.next(partialResponse);
                                }
                            }
                        })
                        .onCompleteResponse(completeResponse -> {
                            String footer = buildResponseFooter(userQuery);
                            sink.next(footer);
                            sink.complete();
                        })
                        .onError(error -> {
                            logger.error("流式响应错误: {}", error.getMessage(), error);
                            sink.next("\n\n⚠️ 响应生成中断，请刷新重试");
                            sink.error(error);
                        })
                        .start();
            });

        } catch (Exception e) {
            logger.error("流式处理查询时发生错误: {}", e.getMessage(), e);
            return Flux.just(generateFallbackResponse(userQuery));
        }
    }



    /**
     * 系统启动时初始化海洋生物知识库向量数据
     */
    private void initializeMarineKnowledgeBase() {
        try {
            logger.info("开始初始化海洋生物知识库向量数据...");

            String[] marineKnowledgeBase = knowledgeBaseService.getMarineBiologyKnowledgeBase();
            String[] marineFaqBase = knowledgeBaseService.getFaqDatabase();

            // 处理主要海洋生物知识库
            for (int i = 0; i < marineKnowledgeBase.length; i++) {
                addKnowledgeToVectorStore(marineKnowledgeBase[i], "海洋生物知识库-" + (i + 1));
            }

            // 处理海洋生物FAQ数据库
            for (int i = 0; i < marineFaqBase.length; i++) {
                addKnowledgeToVectorStore(marineFaqBase[i], "海洋生物FAQ-" + (i + 1));
            }

            isInitialized = true;
            logger.info("海洋生物知识库初始化完成！共加载 {} 条知识条目",
                    marineKnowledgeBase.length + marineFaqBase.length);

        } catch (Exception e) {
            logger.error("海洋生物知识库初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("海洋生物知识库初始化失败", e);
        }
    }

    /**
     * 添加海洋生物知识到向量存储
     */
    private void addKnowledgeToVectorStore(String content, String source) {
        try {
            // 确保内容不为空且有意义
            if (content == null || content.trim().isEmpty()) {
                logger.warn("跳过空内容: {}", source);
                return;
            }

            TextSegment segment = TextSegment.from(content.trim());
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            logger.debug("已添加海洋生物知识条目: {} (长度: {})", source, content.length());
        } catch (Exception e) {
            logger.error("添加海洋生物知识条目失败 [{}]: {}", source, e.getMessage());
        }
    }

    /**
     * 构建响应尾部信息
     */
    private String buildResponseFooter(String userQuery) {
        return """
            
            ---
            🌊 海洋探索提醒：
            • 想了解更多海洋生物？访问海洋生物百科全书
            • 支持海洋保护：关注当地海洋保护组织
            • 科学研究合作：marine.research@example.com
            • 紧急海洋生物救助热线：1-800-MARINE-HELP
            
            🐠 让我们一起保护美丽的海洋世界！
            🔋 Powered by DeepSeek AI Marine Biology Expert
            """;
    }

    /**
     * 格式化最终响应
     */
    private String formatFinalResponse(String aiResponse, String userQuery) {
        return String.format("""
            🐋 海洋生物专家回复：
            
            %s
            
            ---
            🌊 海洋探索提醒：
            • 想了解更多海洋生物？访问海洋生物百科全书
            • 支持海洋保护：关注当地海洋保护组织
            • 科学研究合作：marine.research@example.com
            • 紧急海洋生物救助热线：1-800-MARINE-HELP
            
            🐠 让我们一起保护美丽的海洋世界！
            🔋 Powered by DeepSeek AI Marine Biology Expert
            """, aiResponse);
    }

    /**
     * 降级处理响应
     */
    private String generateFallbackResponse(String userQuery) {
        return String.format("""
            🐋 海洋生物专家（基础模式）：
            
            关于您的问题："%s"
            
            📚 基于海洋生物知识库的基础信息：
            • 海洋哺乳动物：鲸鱼、海豚、海豹等智慧生物
            • 鱼类多样性：从小丑鱼到大白鲨，超过3万种
            • 无脊椎动物：珊瑚、水母、章鱼等奇妙生物
            • 深海生物：发光鱼类、巨型鱿鱼等神秘物种
            • 海洋植物：海带、海草、浮游植物等生态基础
            
            🔄 获得完整专家服务：
            1. 稍后重试，AI专家系统正在恢复
            2. 刷新页面重新提问
            3. 查阅海洋生物百科全书获得详细信息
            
            📖 推荐资源：
            🌐 海洋生物百科：www.marinebiologyencyclopedia.com
            📱 海洋探索APP：搜索"Marine Life Explorer"
            🏛️ 参观当地海洋馆获得更直观的了解
            
            ⚡ 专家AI服务将在几分钟内恢复正常
            """, userQuery);
    }

    /**
     * 获取系统状态
     */
    public String getSystemStatus() {
        return String.format("""
            📊 海洋生物专家系统状态：
            • 初始化状态：%s
            • 海洋生物知识库条目：%d+
            • 最大检索结果：%d
            • 最小相似度：%.2f
            • DeepSeek模型：%s
            • 标准模型状态：%s
            • 流式模型状态：%s
            • 海洋生物专家助手状态：%s
            • RAG检索器状态：%s
            • 运行状态：正常 ✅
            • AI引擎：DeepSeek AI Marine Biology Expert
            • 专业领域：海洋生物学、生态学、保护生物学
            """,
                isInitialized ? "已完成" : "进行中",
                knowledgeBaseService.getMarineBiologyKnowledgeBase().length +
                        knowledgeBaseService.getFaqDatabase().length,
                ragProperties.getMaxResults(),
                ragProperties.getMinScore(),
                deepseekModel,
                chatModel != null ? "已连接" : "未连接",
                streamingChatModel != null ? "已连接" : "未连接",
                marineBiologyExpert != null ? "已初始化" : "未初始化",
                contentRetriever != null ? "已配置" : "未配置"
        );
    }

    /**
     * 清理资源和重置聊天记忆
     */
    public void resetChatMemory() {
        try {
            // 重新初始化海洋生物专家助手以清除聊天记忆
            initializeMarineBiologyExpert();
            logger.info("聊天记忆已重置，海洋生物知识RAG功能保持正常");
        } catch (Exception e) {
            logger.error("重置聊天记忆失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取海洋生物专家助手实例
     */
    public MarineBiologyExpert getMarineBiologyExpert() {
        return marineBiologyExpert;
    }

    /**
     * 获取内容检索器实例
     */
    public ContentRetriever getContentRetriever() {
        return contentRetriever;
    }
}