package com.example.springaialibaba.service;

/**
 * 当工具调用需要人工审批时抛出此异常。
 * <p>
 * 这是一个**流程控制异常**（非错误异常），用于中断 AgentService 中的
 * tool call 循环，将控制权交回 Controller 层，由 Controller 返回
 * "需要审批" 的响应给前端。
 * </p>
 */
public class ToolApprovalRequiredException extends RuntimeException {

    private final PendingApproval pendingApproval;

    public ToolApprovalRequiredException(PendingApproval pendingApproval) {
        super("工具调用需要人工审批: " + pendingApproval.getToolCallSummaries());
        this.pendingApproval = pendingApproval;
    }

    public PendingApproval getPendingApproval() {
        return pendingApproval;
    }
}
