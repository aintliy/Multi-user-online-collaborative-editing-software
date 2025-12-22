-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_editing_db
-- =====================================================
-- 多人在线协作编辑软件 PostgreSQL 数据库架构
-- 基于开发手册规范完全重构版本（2025-12-22）
-- =====================================================

-- 清理所有旧表（按依赖关系倒序）
DROP TABLE IF EXISTS operation_logs CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS document_invite_links CASCADE;
DROP TABLE IF EXISTS document_workspace_requests CASCADE;
DROP TABLE IF EXISTS document_collaborators CASCADE;
DROP TABLE IF EXISTS document_versions CASCADE;
DROP TABLE IF EXISTS user_friends CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================
-- 1. 用户表 users
-- =====================================================
CREATE TABLE users (
  id           BIGSERIAL PRIMARY KEY,
  -- 对外展示与搜索使用的随机不可变 ID，例如 "u_9f3a2c7b"
  public_id    VARCHAR(32) NOT NULL UNIQUE,
  username     VARCHAR(50)  NOT NULL,
  email        VARCHAR(100) NOT NULL UNIQUE,
  phone        VARCHAR(20),
  password     VARCHAR(255) NOT NULL,
  avatar_url   VARCHAR(255),
  profile      TEXT,
  status       VARCHAR(20)  DEFAULT 'active',       -- active / disabled
  role         VARCHAR(20)  DEFAULT 'USER',         -- ADMIN / USER（系统角色）
  created_at   TIMESTAMPTZ  DEFAULT NOW(),
  updated_at   TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_public_id ON users(public_id);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.public_id IS '对外公开的随机不可变用户ID，用于搜索和仓库URL';
COMMENT ON COLUMN users.role IS '系统角色：ADMIN-管理员，USER-普通用户';
COMMENT ON COLUMN users.status IS '用户状态：active-正常，disabled-禁用';

-- =====================================================
-- 2. 文档表 documents
-- =====================================================
CREATE TABLE documents (
  id             BIGSERIAL PRIMARY KEY,
  title          VARCHAR(255) NOT NULL,
  owner_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content        TEXT,                          -- 当前最新「已提交」内容
  doc_type       VARCHAR(20) DEFAULT 'doc',     -- doc / sheet / slide / markdown
  visibility     VARCHAR(20) DEFAULT 'private', -- private / public
  tags           VARCHAR(255),                  -- 标签，逗号分隔
  folder_id      VARCHAR(50),                   -- 文件夹ID（可选）
  status         VARCHAR(20) DEFAULT 'active',
  forked_from_id BIGINT REFERENCES documents(id) ON DELETE SET NULL, -- 克隆来源文档ID
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_documents_owner      ON documents(owner_id);
CREATE INDEX idx_documents_visibility ON documents(visibility);
CREATE INDEX idx_documents_forked     ON documents(forked_from_id);
CREATE INDEX idx_documents_status     ON documents(status);

COMMENT ON TABLE documents IS '文档表';
COMMENT ON COLUMN documents.visibility IS 'private-私有（仅所有者和协作者可见），public-公开（任何人可查看）';
COMMENT ON COLUMN documents.forked_from_id IS '克隆来源文档ID，克隆后与原文档权限完全解耦';

-- =====================================================
-- 3. 文档版本表 document_versions（提交记录）
-- =====================================================
CREATE TABLE document_versions (
  id             BIGSERIAL PRIMARY KEY,
  document_id    BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  version_no     INTEGER NOT NULL,                -- 文档内部递增版本号：1,2,3,...
  content        TEXT   NOT NULL,                 -- 本次提交的完整内容快照
  commit_message VARCHAR(255),                    -- 提交说明，类似 Git commit message
  created_by     BIGINT REFERENCES users(id) ON DELETE SET NULL, -- 发起提交的用户
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (document_id, version_no)
);

CREATE INDEX idx_doc_versions_document ON document_versions(document_id);
CREATE INDEX idx_doc_versions_created_by ON document_versions(created_by);

COMMENT ON TABLE document_versions IS '文档版本表（Git风格提交记录）';
COMMENT ON COLUMN document_versions.version_no IS '文档内部版本号，从1开始递增';
COMMENT ON COLUMN document_versions.commit_message IS '提交说明（类似Git commit message）';

-- =====================================================
-- 4. 文档协作者表 document_collaborators
-- =====================================================
CREATE TABLE document_collaborators (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role         VARCHAR(20) NOT NULL,          -- EDITOR / VIEWER
  invited_by   BIGINT REFERENCES users(id) ON DELETE SET NULL,   -- 邀请人（通常为 owner）
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  removed_at   TIMESTAMPTZ,
  UNIQUE (document_id, user_id)
);

CREATE INDEX idx_doc_collaborators_document ON document_collaborators(document_id);
CREATE INDEX idx_doc_collaborators_user ON document_collaborators(user_id);

COMMENT ON TABLE document_collaborators IS '文档协作者表（工作区成员）';
COMMENT ON COLUMN document_collaborators.role IS 'EDITOR-可编辑，VIEWER-只读';
COMMENT ON COLUMN document_collaborators.removed_at IS '被移除时间（踢出工作区）';

-- =====================================================
-- 5. 文档协作申请表 document_workspace_requests
-- =====================================================
CREATE TABLE document_workspace_requests (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  applicant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING / APPROVED / REJECTED
  message      VARCHAR(255),                          -- 申请理由（可选）
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  handled_at   TIMESTAMPTZ,
  handled_by   BIGINT REFERENCES users(id) ON DELETE SET NULL  -- 处理人（通常为 owner）
);

CREATE INDEX idx_workspace_requests_document ON document_workspace_requests(document_id);
CREATE INDEX idx_workspace_requests_applicant ON document_workspace_requests(applicant_id);
CREATE INDEX idx_workspace_requests_status ON document_workspace_requests(status);

COMMENT ON TABLE document_workspace_requests IS '文档协作申请表（用户申请加入公开文档的工作区）';

-- =====================================================
-- 6. 文档邀请链接表 document_invite_links（可选增强）
-- =====================================================
CREATE TABLE document_invite_links (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  token        VARCHAR(64) NOT NULL UNIQUE,          -- 邀请链接中的随机 token
  role         VARCHAR(20) NOT NULL,                 -- 通过链接加入后的默认角色：EDITOR / VIEWER
  max_uses     INTEGER,                              -- 最大可用次数（NULL 表示无限制）
  used_count   INTEGER DEFAULT 0,
  expires_at   TIMESTAMPTZ,                          -- 过期时间
  created_by   BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_invite_links_document ON document_invite_links(document_id);
CREATE INDEX idx_invite_links_token ON document_invite_links(token);

COMMENT ON TABLE document_invite_links IS '文档邀请链接表（可选功能）';

-- =====================================================
-- 7. 好友关系表 user_friends
-- =====================================================
CREATE TABLE user_friends (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  friend_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING / ACCEPTED / REJECTED / BLOCKED
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  updated_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_user_friends_user   ON user_friends(user_id);
CREATE INDEX idx_user_friends_friend ON user_friends(friend_id);
CREATE INDEX idx_user_friends_status ON user_friends(status);

COMMENT ON TABLE user_friends IS '好友关系表';
COMMENT ON COLUMN user_friends.status IS 'PENDING-待处理，ACCEPTED-已接受，REJECTED-已拒绝，BLOCKED-已屏蔽';

-- =====================================================
-- 8. 评论表 comments
-- =====================================================
CREATE TABLE comments (
  id                   BIGSERIAL PRIMARY KEY,
  document_id          BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content              TEXT NOT NULL,
  reply_to_comment_id  BIGINT REFERENCES comments(id) ON DELETE SET NULL,
  range_info           TEXT,               -- JSON 字符串，前端解释
  status               VARCHAR(20) DEFAULT 'open', -- open / resolved
  created_at           TIMESTAMPTZ DEFAULT NOW(),
  updated_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_comments_document ON comments(document_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_reply_to ON comments(reply_to_comment_id);

COMMENT ON TABLE comments IS '评论表（文档批注与回复）';
COMMENT ON COLUMN comments.range_info IS '选中范围信息（JSON格式）';
COMMENT ON COLUMN comments.status IS 'open-未解决，resolved-已解决';

-- =====================================================
-- 9. 文档内聊天消息表 chat_messages（可选持久化）
-- =====================================================
CREATE TABLE chat_messages (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  sender_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content      TEXT   NOT NULL,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_document ON chat_messages(document_id);
CREATE INDEX idx_chat_messages_sender   ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);

COMMENT ON TABLE chat_messages IS '文档内聊天消息表（可选持久化，用于聊天历史）';

-- =====================================================
-- 10. 通知表 notifications
-- =====================================================
CREATE TABLE notifications (
  id            BIGSERIAL PRIMARY KEY,
  receiver_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type          VARCHAR(50) NOT NULL,   -- COMMENT / TASK / PERMISSION / FRIEND_REQUEST / WORKSPACE_REQUEST 等
  reference_id  BIGINT,                 -- 关联业务实体 ID
  content       TEXT NOT NULL,
  is_read       BOOLEAN DEFAULT FALSE,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_notifications_receiver ON notifications(receiver_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

COMMENT ON TABLE notifications IS '通知表';
COMMENT ON COLUMN notifications.type IS '通知类型：COMMENT-评论，TASK-任务，FRIEND_REQUEST-好友请求，WORKSPACE_REQUEST-协作申请等';

-- =====================================================
-- 11. 任务表 tasks
-- =====================================================
CREATE TABLE tasks (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  creator_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assignee_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  status       VARCHAR(20) DEFAULT 'TODO', -- TODO / DOING / DONE
  due_date     DATE,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_tasks_document ON tasks(document_id);
CREATE INDEX idx_tasks_creator ON tasks(creator_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_status ON tasks(status);

COMMENT ON TABLE tasks IS '任务表（基于文档的任务分配与跟踪）';
COMMENT ON COLUMN tasks.status IS 'TODO-待办，DOING-进行中，DONE-已完成';

-- =====================================================
-- 12. 操作日志表 operation_logs
-- =====================================================
CREATE TABLE operation_logs (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  action       VARCHAR(50) NOT NULL, -- CREATE_DOC / DELETE_DOC / UPDATE_PERMISSION 等
  target_type  VARCHAR(50) NOT NULL, -- DOC / USER / ROLE / PERMISSION
  target_id    BIGINT,
  detail       TEXT,
  ip_address   VARCHAR(50),          -- 操作IP地址
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_op_logs_user ON operation_logs(user_id);
CREATE INDEX idx_op_logs_action ON operation_logs(action);
CREATE INDEX idx_op_logs_target ON operation_logs(target_type, target_id);
CREATE INDEX idx_op_logs_created_at ON operation_logs(created_at);

COMMENT ON TABLE operation_logs IS '操作日志表（审计与追踪）';
COMMENT ON COLUMN operation_logs.action IS '操作类型：CREATE_DOC-创建文档，DELETE_DOC-删除文档，UPDATE_PERMISSION-更新权限等';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认管理员账号（密码: admin123，需使用BCrypt加密）
-- 注意：实际部署时应由DBA在数据库中创建，密码需要用BCrypt加密
INSERT INTO users (public_id, username, email, password, role, status) 
VALUES 
  ('u_admin001', 'admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ADMIN', 'active');
-- 注意：上面的密码哈希是示例，实际应用需要重新生成

COMMENT ON DATABASE postgres IS '多人在线协作编辑系统数据库';
