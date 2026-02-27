package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;

public class ToolAwareChatMemoryAdvisor implements CallAdvisor {
    private final ChatMemory chatMemory;
    private final String conversationId;

    public ToolAwareChatMemoryAdvisor(ChatMemory chatMemory, String conversationId) {
        this.chatMemory = chatMemory;
        this.conversationId = conversationId;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 发起请求前，把用户的消息存入 DB 和 Prompt 
        // （为防止重复，可以检查是否是第一次）
        this.chatMemory.add(this.conversationId, request.prompt().getInstructions());
        
        // 2. 将控制权交给后续的 ToolCallAdvisor 和 ChatModel
        ChatClientResponse response = chain.nextCall(request);
        
        // 3. 拿到最终回答后。我们可以从 response 的完整历史中，提取新增加的节点（包括 ToolResponseMessage）
        // 或者是直接依赖框架能够将其完整返回，把新增的所有的 Message 追加到 ChatMemory 中
        // (注：需要从 response.chatResponse() 中解析最新的消息记录存入 chatMemory)
        
        return response;
    }
    
    @Override
    public int getOrder() {
        return 0; // 最外层
    }
}
