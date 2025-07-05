package com.gec.marine.controller;

import com.gec.marine.config.AssistantConfig;
import com.gec.marine.service.ChatService;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 聊天控制器
 * 提供REST API接口，处理HTTP请求
 *
 * @author Your Name
 * @version 1.0
 */
@RestController  // Spring注解：标记为REST控制器，自动序列化返回值为JSON
@RequestMapping("/chat")  // 设置控制器的基础路径
public class ChatController {

    /**
     * 自动注入聊天服务
     * Spring会自动找到ChatService的实例并注入
     */
    @Autowired
    private ChatService chatService;
    @Autowired
    private AssistantConfig.Assistant  assistant;

    /**
     * POST接口：接收用户消息并返回AI回复
     *
     * @param message 请求体中的用户消息
     * @return AI模型的回复
     */
    @GetMapping("/sendmessage")
    public String chat(@RequestParam("message") String message) {
        // 调用服务层方法处理聊天逻辑
        return chatService.chat(message);
    }

    @GetMapping("/sendmessageByAssistant")
    public String sendmessageByAssistant(@RequestParam("message") String message) {
        // 调用服务层方法处理聊天逻辑
        return assistant.chat(message);
    }

    /**
     * GET接口：测试用的hello接口
     * 用于快速验证系统是否正常工作
     *
     * @return AI对"你好，你是谁？"的回复
     */
    @GetMapping("/hello")  // 映射GET请求到/api/chat/hello
    public String hello() {
        // 发送固定的问候消息给AI
        return chatService.chat("你好，你是谁？");
    }

    // 定义HTTP接口端点，处理带有记忆功能的流式聊天请求
    @RequestMapping(value = "/memory_steam_chat",produces = "text/stream;charset=utf-8")
// 返回Flux<String>类型实现流式响应，参数message默认值为"我是谁"
    public Flux<String> memoryStreamChat(
            // 从请求参数获取用户消息，默认值"我是谁"
            @RequestParam(defaultValue = "我是谁") String message,
            // 注入HttpServletResponse对象（虽然响应式编程中通常不直接使用）
            HttpServletResponse response) {

        // 调用助手服务的流式接口，获取TokenStream对象
        TokenStream stream = assistant.stream(message);

        // 创建Flux流式响应
        return Flux.create(sink -> {
            // 设置部分响应回调：每次收到部分响应时通过sink发送数据
            stream.onPartialResponse(s -> sink.next(s))
                    // 设置完成回调：当收到完成信号时关闭流
                    .onCompleteResponse(c-> sink.complete())
                    // 设置错误回调：发生错误时传递错误信号 它的作用是将 sink 对象的 error 方法作为函数式接口的实现传递进去。
                    .onError(sink::error)//stream.onError(error -> sink.error(error));
                    // 启动流处理
                    .start();
        });
    }
}