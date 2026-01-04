-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_db
-- 为用户表添加最后登录时间字段
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WiTH TIME ZONE;

-- 添加索引以支持活跃用户查询
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login_at);

COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
