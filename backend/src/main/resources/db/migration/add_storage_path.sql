-- Active: 1766324267089@@127.0.0.1@5432@postgres@collab_db
-- =====================================================
-- 数据库迁移脚本：添加文档物理存储路径功能
-- 迁移日期: 2024-12-25
-- 目标: 为现有 documents 表添加 storage_path 字段
-- =====================================================

-- 1. 添加 storage_path 字段
ALTER TABLE documents 
ADD COLUMN storage_path VARCHAR(512);

-- 2. 添加字段注释
COMMENT ON COLUMN documents.storage_path IS '物理文件存储相对路径，格式: {ownerId}/{folderId}/，folderId为空表示用户根目录';

-- 3. (可选) 为现有文档初始化 storage_path
-- 注意：此脚本会为所有现有文档创建物理目录路径
-- 执行前请确保后端 FileStorageService 已部署并运行
/*
UPDATE documents 
SET storage_path = CONCAT(
    owner_id, 
    '/',
    COALESCE(CAST(folder_id AS VARCHAR), 'root'),
    '/'
)
WHERE storage_path IS NULL;
*/

-- 说明：
-- - 上述 UPDATE 语句用于为历史数据初始化 storage_path
-- - 新创建的文档会自动通过后端服务设置 storage_path
-- - 如果需要为历史数据创建实际的物理目录，需要编写额外的迁移脚本或手动创建
