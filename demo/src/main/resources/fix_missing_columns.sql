-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_editing_db
-- 修复 documents 表缺失字段 tags 和 folder_id 的问题
ALTER TABLE documents ADD COLUMN tags VARCHAR(255);
ALTER TABLE documents ADD COLUMN folder_id VARCHAR(50);
