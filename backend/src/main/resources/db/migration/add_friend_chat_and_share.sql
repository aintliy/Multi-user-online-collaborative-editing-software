-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_db
-- =====================================================
-- 好友私聊消息表 (friend_messages)
-- =====================================================
CREATE TABLE IF NOT EXISTS friend_messages (
  id           BIGSERIAL PRIMARY KEY,
  sender_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  receiver_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content      TEXT NOT NULL,
  message_type VARCHAR(20) DEFAULT 'TEXT',  -- TEXT-普通文本, SHARE_LINK-分享链接
  share_link_id BIGINT,
  is_read      BOOLEAN DEFAULT FALSE,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_friend_messages_sender ON friend_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_friend_messages_receiver ON friend_messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_friend_messages_created ON friend_messages(created_at DESC);

COMMENT ON TABLE friend_messages IS '好友私聊消息表';
COMMENT ON COLUMN friend_messages.message_type IS '消息类型: TEXT-普通文本, SHARE_LINK-分享链接';
COMMENT ON COLUMN friend_messages.share_link_id IS '关联的分享链接ID';

-- =====================================================
-- 文档分享链接表 (document_share_links)
-- 一次性使用，仅限好友聊天分享
-- =====================================================
CREATE TABLE IF NOT EXISTS document_share_links (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  token        VARCHAR(64) NOT NULL UNIQUE,
  created_by   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  is_used      BOOLEAN DEFAULT FALSE,
  used_by      BIGINT REFERENCES users(id),
  used_at      TIMESTAMP,
  expires_at   TIMESTAMP NOT NULL,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_share_links_document ON document_share_links(document_id);
CREATE INDEX IF NOT EXISTS idx_share_links_token ON document_share_links(token);
CREATE INDEX IF NOT EXISTS idx_share_links_created_by ON document_share_links(created_by);

COMMENT ON TABLE document_share_links IS '文档分享链接表（一次性使用，仅限好友聊天）';
COMMENT ON COLUMN document_share_links.token IS '分享链接token';
COMMENT ON COLUMN document_share_links.is_used IS '是否已使用';
COMMENT ON COLUMN document_share_links.used_by IS '使用人ID';
COMMENT ON COLUMN document_share_links.expires_at IS '过期时间（24小时后过期）';
