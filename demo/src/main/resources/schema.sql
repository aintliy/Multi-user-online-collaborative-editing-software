-- 多人在线协作编辑软件 PostgreSQL 数据库初始化脚本
-- 基于《数据库设计.md》规范

-- =====================================================
-- 1. 用户表 users
-- =====================================================
CREATE TABLE users (
  id           BIGSERIAL PRIMARY KEY,
  username     VARCHAR(50)  NOT NULL,
  email        VARCHAR(100) NOT NULL UNIQUE,
  phone        VARCHAR(20),
  password     VARCHAR(255) NOT NULL,
  avatar_url   VARCHAR(255),
  profile      TEXT,
  status       VARCHAR(20)  DEFAULT 'active', -- active / disabled
  created_at   TIMESTAMPTZ  DEFAULT NOW(),
  updated_at   TIMESTAMPTZ  DEFAULT NOW()
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.status IS '用户状态: active-正常, disabled-禁用';

-- =====================================================
-- 2. 角色表 roles
-- =====================================================
CREATE TABLE roles (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(50) NOT NULL UNIQUE, -- ADMIN / EDITOR / VIEWER
  name        VARCHAR(100) NOT NULL,
  description TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  updated_at  TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE roles IS '角色表';

-- 插入初始角色
INSERT INTO roles (code, name, description) VALUES 
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('EDITOR', '编辑者', '可以编辑文档的用户'),
('VIEWER', '查看者', '只能查看文档的用户');

-- =====================================================
-- 3. 权限表 permissions
-- =====================================================
CREATE TABLE permissions (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(100) NOT NULL UNIQUE, -- 如 DOC_VIEW, DOC_EDIT
  name        VARCHAR(100) NOT NULL,
  description TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  updated_at  TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE permissions IS '权限表';

-- 插入初始权限
INSERT INTO permissions (code, name, description) VALUES 
('DOC_VIEW', '查看文档', '可以查看文档内容'),
('DOC_EDIT', '编辑文档', '可以编辑文档内容'),
('DOC_DELETE', '删除文档', '可以删除文档'),
('USER_MANAGE', '用户管理', '可以管理系统用户'),
('ROLE_MANAGE', '角色管理', '可以管理角色和权限');

-- =====================================================
-- 4. 用户-角色关系表 user_roles
-- =====================================================
CREATE TABLE user_roles (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id    BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (user_id, role_id)
);

COMMENT ON TABLE user_roles IS '用户-角色关系表（多对多）';
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- =====================================================
-- 5. 角色-权限关系表 role_permissions
-- =====================================================
CREATE TABLE role_permissions (
  id           BIGSERIAL PRIMARY KEY,
  role_id      BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (role_id, permission_id)
);

COMMENT ON TABLE role_permissions IS '角色-权限关系表（多对多）';
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);

-- 配置默认角色权限
-- ADMIN 拥有所有权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- EDITOR 拥有查看和编辑权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.code = 'EDITOR' AND p.code IN ('DOC_VIEW', 'DOC_EDIT');

-- VIEWER 只有查看权限
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.code = 'VIEWER' AND p.code = 'DOC_VIEW';

-- =====================================================
-- 6. 文档表 documents
-- =====================================================
CREATE TABLE documents (
  id          BIGSERIAL PRIMARY KEY,
  title       VARCHAR(255) NOT NULL,
  owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content     TEXT,                 -- 当前最新内容
  doc_type    VARCHAR(20) DEFAULT 'doc', -- doc / sheet / slide
  status      VARCHAR(20) DEFAULT 'active',
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  updated_at  TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE documents IS '文档表';
COMMENT ON COLUMN documents.doc_type IS '文档类型: doc-文档, sheet-表格, slide-演示';
CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_updated ON documents(updated_at);

-- =====================================================
-- 7. 文档版本表 document_versions
-- =====================================================
CREATE TABLE document_versions (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  version_no   INTEGER NOT NULL,
  content      TEXT NOT NULL,
  created_by   BIGINT REFERENCES users(id),
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (document_id, version_no)
);

COMMENT ON TABLE document_versions IS '文档版本表';
CREATE INDEX idx_doc_versions_document ON document_versions(document_id);

-- =====================================================
-- 8. 文档权限表 document_permissions
-- =====================================================
CREATE TABLE document_permissions (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role         VARCHAR(20) NOT NULL, -- EDITOR / VIEWER
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (document_id, user_id)
);

COMMENT ON TABLE document_permissions IS '文档权限表';
COMMENT ON COLUMN document_permissions.role IS '文档级权限: EDITOR-编辑者, VIEWER-查看者';
CREATE INDEX idx_doc_permissions_document ON document_permissions(document_id);
CREATE INDEX idx_doc_permissions_user ON document_permissions(user_id);

-- =====================================================
-- 9. 评论表 comments
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

COMMENT ON TABLE comments IS '评论表';
COMMENT ON COLUMN comments.range_info IS '选中范围信息（JSON）';
CREATE INDEX idx_comments_document ON comments(document_id);
CREATE INDEX idx_comments_user ON comments(user_id);

-- =====================================================
-- 10. 通知表 notifications
-- =====================================================
CREATE TABLE notifications (
  id            BIGSERIAL PRIMARY KEY,
  receiver_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type          VARCHAR(50) NOT NULL,   -- COMMENT / TASK / PERMISSION 等
  reference_id  BIGINT,                 -- 关联业务实体 ID
  content       TEXT NOT NULL,
  is_read       BOOLEAN DEFAULT FALSE,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE notifications IS '通知表';
COMMENT ON COLUMN notifications.type IS '通知类型: COMMENT-评论, TASK-任务, PERMISSION-权限变更';
CREATE INDEX idx_notifications_receiver ON notifications(receiver_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);

-- =====================================================
-- 11. 任务表 tasks（选做）
-- =====================================================
CREATE TABLE tasks (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  creator_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assignee_id  BIGINT REFERENCES users(id),
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  status       VARCHAR(20) DEFAULT 'todo', -- todo / doing / done
  due_date     DATE,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  updated_at   TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE tasks IS '任务表（选做）';
CREATE INDEX idx_tasks_document ON tasks(document_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);

-- =====================================================
-- 12. 操作日志表 operation_logs
-- =====================================================
CREATE TABLE operation_logs (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id),
  action       VARCHAR(50) NOT NULL, -- CREATE_DOC / DELETE_DOC / UPDATE_PERMISSION 等
  target_type  VARCHAR(50) NOT NULL, -- DOC / USER / ROLE / PERMISSION
  target_id    BIGINT,
  detail       TEXT,
  ip_address   VARCHAR(50),
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE operation_logs IS '操作日志表';
CREATE INDEX idx_op_logs_user ON operation_logs(user_id);
CREATE INDEX idx_op_logs_action ON operation_logs(action);
CREATE INDEX idx_op_logs_created ON operation_logs(created_at);

-- =====================================================
-- 初始化完成
-- =====================================================
