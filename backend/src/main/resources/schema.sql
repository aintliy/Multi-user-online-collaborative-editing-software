-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_db
-- =====================================================
-- 多人在线协作编辑软件 - PostgreSQL 数据库建表脚本
-- 创建时间: 2024-12-24
-- 数据库: PostgreSQL
-- =====================================================

DROP TABLE IF EXISTS operation_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS user_friends CASCADE;
DROP TABLE IF EXISTS document_workspace_requests CASCADE;
DROP TABLE IF EXISTS document_collaborators CASCADE;
DROP TABLE IF EXISTS document_versions CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS document_folders CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS friend_messages CASCADE;



-- =====================================================
-- 1. 用户表 (users)
-- =====================================================
CREATE TABLE users (
  id           BIGSERIAL PRIMARY KEY,
  -- 对外展示与搜索使用的随机不可变 ID，例如 "uid_9f3a2c7b"
  public_id    VARCHAR(32) NOT NULL UNIQUE,
  username     VARCHAR(50)  NOT NULL,
  email        VARCHAR(100) NOT NULL UNIQUE,
  password     VARCHAR(255) NOT NULL,
  avatar_url   VARCHAR(255),
  profile      TEXT,
  status       VARCHAR(20)  DEFAULT 'ACTIVE',       -- ACTIVE / DISABLED
  role         VARCHAR(20)  DEFAULT 'USER',         -- ADMIN / USER（系统角色）
  created_at   TIMESTAMP  DEFAULT NOW(),
  updated_at   TIMESTAMP  DEFAULT NOW()
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_public_id ON users(public_id);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.public_id IS '对外展示的随机不可变用户ID';
COMMENT ON COLUMN users.role IS '系统角色: ADMIN-管理员, USER-普通用户';
COMMENT ON COLUMN users.status IS '用户状态: ACTIVE-正常, DISABLED-已禁用';

-- =====================================================
-- 2. 文档文件夹表 (document_folders)
-- =====================================================
CREATE TABLE document_folders (
  id           BIGSERIAL PRIMARY KEY,
  owner_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name         VARCHAR(255) NOT NULL,
  parent_id    BIGINT REFERENCES document_folders(id) ON DELETE CASCADE,
  status       VARCHAR(20) DEFAULT 'ACTIVE',
  created_at   TIMESTAMP DEFAULT NOW(),
  updated_at   TIMESTAMP DEFAULT NOW(),
  CONSTRAINT uq_folder_name_per_parent UNIQUE (owner_id, parent_id, name)
);

CREATE INDEX idx_doc_folders_owner ON document_folders(owner_id);
CREATE INDEX idx_doc_folders_parent ON document_folders(parent_id);

COMMENT ON TABLE document_folders IS '文档文件夹表，支持多级嵌套';
COMMENT ON COLUMN document_folders.parent_id IS '父文件夹ID，NULL表示根目录';

-- =====================================================
-- 3. 文档表 (documents)
-- =====================================================
CREATE TABLE documents (
  id             BIGSERIAL PRIMARY KEY,
  title          VARCHAR(255) NOT NULL,
  owner_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content        TEXT,
  doc_type       VARCHAR(20) DEFAULT 'markdown',
  visibility     VARCHAR(20) DEFAULT 'PRIVATE',  -- PRIVATE-私有, PUBLIC-公开
  tags           VARCHAR(255),
  folder_id      BIGINT REFERENCES document_folders(id) ON DELETE SET NULL,
  status         VARCHAR(20) DEFAULT 'ACTIVE',
  forked_from_id BIGINT REFERENCES documents(id),
  storage_path   VARCHAR(512),
  created_at     TIMESTAMP DEFAULT NOW(),
  updated_at     TIMESTAMP DEFAULT NOW(),
  -- 同一所有者同一文件夹下文档名唯一（逻辑删除时 folder_id 为 NULL，不参与约束）
  CONSTRAINT uq_doc_title_per_folder UNIQUE (owner_id, folder_id, title)
);

CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_visibility ON documents(visibility);
CREATE INDEX idx_documents_forked ON documents(forked_from_id);
CREATE INDEX idx_documents_folder ON documents(folder_id);
CREATE INDEX idx_documents_status ON documents(status);

COMMENT ON TABLE documents IS '文档表';
COMMENT ON COLUMN documents.doc_type IS '文档类型: markdown / txt';
COMMENT ON COLUMN documents.visibility IS '可见性: PRIVATE-私有, PUBLIC-公开';
COMMENT ON COLUMN documents.folder_id IS '所属文件夹ID';
COMMENT ON COLUMN documents.forked_from_id IS '克隆来源文档ID';
COMMENT ON COLUMN documents.storage_path IS '物理文件存储相对路径，格式: {ownerId}/{folderId}/';

-- =====================================================
-- 4. 文档版本表 (document_versions)
-- =====================================================
CREATE TABLE document_versions (
  id             BIGSERIAL PRIMARY KEY,
  document_id    BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  version_no     INTEGER NOT NULL,
  content        TEXT   NOT NULL,
  commit_message VARCHAR(255),
  created_by     BIGINT REFERENCES users(id),
  created_at     TIMESTAMP DEFAULT NOW(),
  CONSTRAINT uq_doc_version UNIQUE (document_id, version_no)
);

CREATE INDEX idx_doc_versions_document ON document_versions(document_id);
CREATE INDEX idx_doc_versions_created_by ON document_versions(created_by);

COMMENT ON TABLE document_versions IS '文档版本表，类似Git提交历史';
COMMENT ON COLUMN document_versions.version_no IS '文档内部递增版本号';
COMMENT ON COLUMN document_versions.commit_message IS '提交说明';

-- =====================================================
-- 5. 文档协作者表 (document_collaborators)
-- =====================================================
CREATE TABLE document_collaborators (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  invited_by   BIGINT REFERENCES users(id),
  created_at   TIMESTAMP DEFAULT NOW(),
  CONSTRAINT uq_doc_collaborator UNIQUE (document_id, user_id)
);

CREATE INDEX idx_doc_collaborators_document ON document_collaborators(document_id);
CREATE INDEX idx_doc_collaborators_user ON document_collaborators(user_id);

COMMENT ON TABLE document_collaborators IS '文档协作者表';
COMMENT ON COLUMN document_collaborators.invited_by IS '邀请人ID';

-- =====================================================
-- 6. 文档协作请求表 (document_workspace_requests)
-- 支持两种类型: APPLY-用户申请加入, INVITE-所有者邀请
-- =====================================================
CREATE TABLE document_workspace_requests (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  applicant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type         VARCHAR(20) NOT NULL DEFAULT 'APPLY',
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  message      VARCHAR(255),
  created_at   TIMESTAMP DEFAULT NOW(),
  handled_at   TIMESTAMP,
  handled_by   BIGINT REFERENCES users(id)
);

CREATE INDEX idx_workspace_requests_document ON document_workspace_requests(document_id);
CREATE INDEX idx_workspace_requests_applicant ON document_workspace_requests(applicant_id);
CREATE INDEX idx_workspace_requests_status ON document_workspace_requests(status);
CREATE INDEX idx_workspace_requests_type ON document_workspace_requests(type);

-- 部分唯一索引：仅当 status = 'PENDING' 时同一用户对同一文档同一类型不能重复请求
CREATE UNIQUE INDEX uq_pending_request ON document_workspace_requests(document_id, applicant_id, type, status) 
  WHERE status = 'PENDING';

COMMENT ON TABLE document_workspace_requests IS '文档协作请求表';
COMMENT ON COLUMN document_workspace_requests.type IS '请求类型: APPLY-用户申请加入, INVITE-所有者邀请';
COMMENT ON COLUMN document_workspace_requests.status IS '请求状态: PENDING-待处理, APPROVED-已通过, REJECTED-已拒绝';

-- =====================================================
-- 7. 好友关系表 (user_friends)
-- =====================================================
CREATE TABLE user_friends (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  friend_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at   TIMESTAMP DEFAULT NOW(),
  updated_at   TIMESTAMP DEFAULT NOW(),
  CONSTRAINT uq_user_friend UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_user_friends_user ON user_friends(user_id);
CREATE INDEX idx_user_friends_friend ON user_friends(friend_id);
CREATE INDEX idx_user_friends_status ON user_friends(status);

COMMENT ON TABLE user_friends IS '好友关系表';
COMMENT ON COLUMN user_friends.status IS '好友状态: PENDING-待确认, ACCEPTED-已接受, REJECTED-已拒绝, BLOCKED-已屏蔽';

-- =====================================================
-- 8. 评论表 (comments)
-- =====================================================
CREATE TABLE comments (
  id                   BIGSERIAL PRIMARY KEY,
  document_id          BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content              TEXT NOT NULL,
  reply_to_comment_id  BIGINT REFERENCES comments(id) ON DELETE SET NULL,
  range_info           TEXT,
  status               VARCHAR(20) DEFAULT 'open',
  created_at           TIMESTAMP DEFAULT NOW(),
  updated_at           TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_comments_document ON comments(document_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_reply_to ON comments(reply_to_comment_id);

COMMENT ON TABLE comments IS '评论表';
COMMENT ON COLUMN comments.range_info IS '选中范围信息（JSON字符串）';
COMMENT ON COLUMN comments.status IS '评论状态: open-打开, resolved-已解决';

-- =====================================================
-- 9. 文档内聊天消息表 (chat_messages)
-- =====================================================
CREATE TABLE chat_messages (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  sender_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content      TEXT   NOT NULL,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_document ON chat_messages(document_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at DESC);

COMMENT ON TABLE chat_messages IS '文档内聊天消息表（持久化）';

-- =====================================================
-- 10. 通知表 (notifications)
-- =====================================================
CREATE TABLE notifications (
  id            BIGSERIAL PRIMARY KEY,
  receiver_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type          VARCHAR(50) NOT NULL,
  reference_id  BIGINT,
  content       TEXT NOT NULL,
  is_read       BOOLEAN DEFAULT FALSE,
  created_at    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notifications_receiver ON notifications(receiver_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

COMMENT ON TABLE notifications IS '通知表';
COMMENT ON COLUMN notifications.type IS '通知类型: COMMENT-评论, PERMISSION-权限等';
COMMENT ON COLUMN notifications.reference_id IS '关联业务实体ID';

-- =====================================================
-- 11 . 操作日志表 (operation_logs)
-- =====================================================
CREATE TABLE operation_logs (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id),
  action       VARCHAR(50) NOT NULL,
  target_type  VARCHAR(50) NOT NULL,
  target_id    BIGINT,
  detail       TEXT,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_op_logs_user ON operation_logs(user_id);
CREATE INDEX idx_op_logs_action ON operation_logs(action);
CREATE INDEX idx_op_logs_created ON operation_logs(created_at DESC);

COMMENT ON TABLE operation_logs IS '操作日志表';
COMMENT ON COLUMN operation_logs.action IS '操作类型: CREATE_DOC, DELETE_DOC, UPDATE_PERMISSION等';
COMMENT ON COLUMN operation_logs.target_type IS '目标类型: DOC-文档, USER-用户, ROLE-角色, PERMISSION-权限';

-- =====================================================
-- 12. 好友私聊消息表 (friend_messages)
-- =====================================================
CREATE TABLE friend_messages (
  id           BIGSERIAL PRIMARY KEY,
  sender_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  receiver_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content      TEXT NOT NULL,
  is_read      BOOLEAN DEFAULT FALSE,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_friend_messages_sender ON friend_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_friend_messages_receiver ON friend_messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_friend_messages_created ON friend_messages(created_at DESC);

COMMENT ON TABLE friend_messages IS '好友私聊消息表';

-- =====================================================
-- 建表完成
-- =====================================================

-- 注意事项：
-- 1. 密码重置令牌和注册验证码使用 Redis 存储，无需创建数据库表
-- 2. Redis 键格式：
--    - 密码重置：pwd_reset:{token} -> userId，TTL 5分钟
--    - 注册验证码：reg_code:{email} -> code，TTL 5分钟
--    - 访问频率限制：rate_limit:{key}，可自定义 TTL
-- 3. 所有时间戳字段使用 TIMESTAMP 类型（不带时区）
-- 4. 外键约束使用 ON DELETE CASCADE 保证数据一致性
-- 5. 部分字段使用 ON DELETE SET NULL 避免级联删除
