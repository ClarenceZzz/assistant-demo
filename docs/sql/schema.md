```sql
-- 1.1 创建聊天记录表 (chat_session)
CREATE TABLE chat_session (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    session_title VARCHAR(200),
    session_category VARCHAR(50),
    session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加注释
COMMENT ON TABLE chat_session IS '存储用户的聊天会话信息';
COMMENT ON COLUMN chat_session.id IS '主键ID';
COMMENT ON COLUMN chat_session.user_id IS '用户身份标识';
COMMENT ON COLUMN chat_session.session_title IS '会话标题';
COMMENT ON COLUMN chat_session.session_category IS '会话分类/标签';
COMMENT ON COLUMN chat_session.session_status IS '会话状态：ACTIVE, ARCHIVED, DELETED';
COMMENT ON COLUMN chat_session.created_at IS '创建时间';
COMMENT ON COLUMN chat_session.updated_at IS '最后更新时间';

-- 创建索引
-- 经常需要根据用户ID查询其所有会话，并按时间排序
CREATE INDEX idx_chat_session_user_id_created_at ON chat_session(user_id, created_at DESC);
-- 用于按状态筛选
CREATE INDEX idx_chat_session_status ON chat_session(session_status);

-- 创建触发器，在更新 chat_session 表时自动更新 updated_at 字段
CREATE TRIGGER trigger_chat_session_updated_at
BEFORE UPDATE ON chat_session
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 1.2 创建对话记录表 (chat_message)
CREATE TABLE chat_message (
                              id BIGSERIAL PRIMARY KEY,
                              session_id BIGINT NOT NULL,
                              role VARCHAR(20) NOT NULL,
                              content TEXT NOT NULL,
                              retrieval_context JSONB, -- 使用 JSONB 类型以获得更好的性能和查询能力
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 设置外键约束，当 chat_session 中的会话被删除时，关联的 message 也一并删除
                              CONSTRAINT fk_session
                                  FOREIGN KEY(session_id)
                                      REFERENCES chat_session(id)
                                      ON DELETE CASCADE
);

-- 添加注释
COMMENT ON TABLE chat_message IS '存储具体的对话内容';
COMMENT ON COLUMN chat_message.id IS '主键ID';
COMMENT ON COLUMN chat_message.session_id IS '关联的 chat_session 表ID';
COMMENT ON COLUMN chat_message.role IS '角色：USER, ASSISTANT';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.retrieval_context IS '检索上下文（JSONB格式存储Document信息）';
COMMENT ON COLUMN chat_message.created_at IS '创建时间';

-- 创建索引
-- 查询一个会话下的所有消息时，需要按 session_id 和 created_at 排序
CREATE INDEX idx_chat_message_session_id_created_at ON chat_message(session_id, created_at ASC);
```