package com.example.springaialibaba.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 待审批记录存储。
 * <p>
 * 当前实现：内存存储（ConcurrentHashMap），适合单机场景。
 * 生产环境应替换为 Redis/数据库存储，支持：
 * - 分布式部署（多实例共享审批状态）
 * - 持久化（服务重启不丢失）
 * - TTL 过期自动清理
 * </p>
 */
@Component
public class PendingApprovalStore {

    private static final Logger log = LoggerFactory.getLogger(PendingApprovalStore.class);

    /**
     * 审批记录存储。key = approvalId。
     */
    private final Map<String, PendingApproval> store = new ConcurrentHashMap<>();

    /**
     * 保存待审批记录。
     */
    public void save(PendingApproval approval) {
        store.put(approval.getApprovalId(), approval);
        log.info("【审批存储】保存审批记录: approvalId={}, conversationId={}, 工具={}",
                approval.getApprovalId(),
                approval.getConversationId(),
                approval.getToolCallSummaries().stream()
                        .map(PendingApproval.ToolCallSummary::getToolName)
                        .toList());
    }

    /**
     * 获取并移除待审批记录（一次性消费）。
     *
     * @param approvalId 审批 ID
     * @return 审批记录，如果不存在或已过期返回 null
     */
    public PendingApproval getAndRemove(String approvalId) {
        PendingApproval approval = store.remove(approvalId);
        if (approval == null) {
            log.warn("【审批存储】审批记录不存在: {}", approvalId);
            return null;
        }
        if (approval.isExpired()) {
            log.warn("【审批存储】审批记录已过期: {}, 创建时间: {}",
                    approvalId, approval.getCreatedAt());
            return null;
        }
        return approval;
    }

    /**
     * 获取待审批记录（不移除，用于查询状态）。
     */
    public PendingApproval get(String approvalId) {
        PendingApproval approval = store.get(approvalId);
        if (approval != null && approval.isExpired()) {
            store.remove(approvalId);
            return null;
        }
        return approval;
    }

    /**
     * 获取当前待审批数量。
     */
    public int size() {
        return store.size();
    }
}
