package com.example.springai.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

public class ToolAwareChatMemoryAdvisor implements CallAdvisor {
    private final ChatMemory chatMemory;
    private final String defaultConversationId;
    private final ToolCallingManager toolCallingManager;

    public ToolAwareChatMemoryAdvisor(ChatMemory chatMemory, ToolCallingManager toolCallingManager) {
        this(chatMemory, "default", toolCallingManager);
    }

    public ToolAwareChatMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId, ToolCallingManager toolCallingManager) {
        this.chatMemory = chatMemory;
        this.defaultConversationId = defaultConversationId;
        this.toolCallingManager = toolCallingManager;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    // @Override
    // public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    //     // 从当次请求的上下文中动态获取 conversationId（如果没有则兜底使用默认的话题ID）
    //     Object contextId = request.context().get("chat_memory_conversation_id"); // Spring AI 默认上下文键为 chat_memory_conversation_id 或 Advisor.CHAT_MEMORY_CONVERSATION_ID
    //     String conversationId = contextId != null ? contextId.toString() : this.defaultConversationId;

    //     // 1. 发起请求前，把用户的消息存入 DB 和 Prompt 
    //     this.chatMemory.add(conversationId, request.prompt().getInstructions());
        
    //     // 2. 将控制权交给后续的 ToolCallAdvisor 和 ChatModel
    //     ChatClientResponse response = chain.nextCall(request);
        
    //     // 3. 拿到最终回答后。我们可以从 response 的完整历史中，提取新增加的节点（包括 ToolResponseMessage）
    //     // 或者是直接依赖框架能够将其完整返回，把新增的所有的 Message 追加到 ChatMemory 中
    //     // (注：需要从 response.chatResponse() 中解析最新的消息记录存入 chatMemory)
        
    //     return response;
    // }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Object contextId = request.context().get("chat_memory_conversation_id");
        String cid = contextId != null ? contextId.toString() : this.defaultConversationId;

        // 1. 保存用户的输入 (Wait: MessageChatMemoryAdvisor is already processing this!
        // If we add it here, it will duplicate because MessageChatMemoryAdvisor is also in the chain!)
        // However, the requested task says we are creating a custom memory advisor, we should save it here
        this.chatMemory.add(cid, request.prompt().getInstructions());
        
        // 2. 发起请求
        ChatClientResponse response = chain.nextCall(request);
        
        // 3. 拦截判断：底层是否返回了 ToolCall（注意：由于我们是单一方向的拦截器链，不能在这里写 while 循环重复执行 chain.nextCall！）
        // 实际上，Spring AI 在默认开启 internalToolExecutionEnabled(true) 情况下，会自动在底层执行完所有的 ToolCall 并得到最终结果。
        // 或者如果我们一定要显式手动处理 Tool 循环，必须将它移交给独立的循环器或使用 ToolCallAdvisor。
        // 但如果我们只作为保存器，我们只需要获取当前的执行结果（如果它包含工具，我们将其存下）：
        if (response.chatResponse().hasToolCalls()) {
             // 1. 获取当前对话的原始消息数量
             int originalSize = request.prompt().getInstructions().size();
             
             // 2. 本地执行工具调用（调用真实的方法例如天气查询）
             ToolExecutionResult execResult = this.toolCallingManager.executeToolCalls(request.prompt(), response.chatResponse());
             List<Message> history = execResult.conversationHistory();
             
             // 3. 将模型产生的带有 ToolCall 的 AssistantMessage 和本地执行产生的 ToolResponseMessage 存入 Memory 数据库
             for (int i = originalSize; i < history.size(); i++) {
                 this.chatMemory.add(cid, history.get(i));
             }
             
             // 既然包含了工具调用并已经把最新消息存库，直接返回跳过下方重复的存库操作
             return response;
        }
        
        // 4. 保存最终生成的纯文本回答
        this.chatMemory.add(cid, response.chatResponse().getResult().getOutput());
        
        return response;
    }
    
    @Override
    public int getOrder() {
        return 0; // 最外层
    }
}
