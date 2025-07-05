package com.gec.marine.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    /**
     * 聊天语言模型接口
     * LangChain4j提供的统一聊天模型抽象
     */
    private final ChatLanguageModel chatModel;

    /**
     * 构造函数：初始化聊天模型
     * 采用构造器模式配置OpenAI模型
     */
    public ChatService() {
        // 使用Builder模式创建OpenAI聊天模型实例
        this.chatModel = OpenAiChatModel.builder()
                // 设置API基础URL（这里使用演示地址）
                .baseUrl("https://api.deepseek.com")
                // 设置API密钥（生产环境应从配置文件读取）
                .apiKey("sk-0c650aa2ed834330b0e153bf42347e20")
                // 指定使用的模型名称
                .modelName("deepseek-chat")
                // 构建模型实例
                .build();
    }

    /**
     * 聊天方法：与AI模型进行对话
     *
     * @param message 用户输入的消息
     * @return AI模型的回复
     */
    public String chat(String message) {
        // 调用模型的chat方法，传入用户消息并返回AI回复
        return chatModel.chat(message);
    }
}
