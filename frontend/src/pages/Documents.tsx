import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button,
  Input,
  Card,
  List,
  Tag,
  Space,
  Dropdown,
  Modal,
  Form,
  Select,
  Tree,
  Empty,
  message,
  Tooltip,
  Row,
  Col,
  Breadcrumb,
  Divider,
} from 'antd';
import {
  PlusOutlined,
  FileTextOutlined,
  FolderOutlined,
  FolderAddOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  ShareAltOutlined,
  LockOutlined,
  GlobalOutlined,
  HomeOutlined,
  RightOutlined,
} from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import { documentApi, folderApi, collaboratorApi } from '../api';
import type { Document, Folder } from '../types';
import dayjs from 'dayjs';
import './Documents.scss';

const { Search } = Input;

const Documents: React.FC = () => {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFolderId, setSelectedFolderId] = useState<number | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [currentPath, setCurrentPath] = useState<{ id: number | null; name: string }[]>([{ id: null, name: '根目录' }]);
  
  // Modals
  const [createDocModalOpen, setCreateDocModalOpen] = useState(false);
  const [createFolderModalOpen, setCreateFolderModalOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  
  const [form] = Form.useForm();
  const [folderForm] = Form.useForm();

  useEffect(() => {
    fetchFolders();
    fetchDocuments();
  }, [selectedFolderId, searchKeyword]);

  const fetchFolders = async () => {
    try {
      const data = await folderApi.getTree();
      setFolders(data);
    } catch (error) {
      console.error('Failed to fetch folders:', error);
    }
  };

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const params: any = {};
      if (selectedFolderId !== null) {
        params.folderId = selectedFolderId;
      }
      if (searchKeyword) {
        params.keyword = searchKeyword;
      }
      const data = await documentApi.getList(params);
      setDocuments(data.content);
    } catch (error) {
      console.error('Failed to fetch documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleImportDocument = async (file: File) => {
    try {
      const ext = file.name.split('.').pop()?.toLowerCase();
      if (!ext || !['md', 'markdown', 'txt'].includes(ext)) {
        message.error('仅支持导入 Markdown(.md) 或 TXT 文件');
        return;
      }
      const doc = await documentApi.import(file, selectedFolderId);
      message.success('导入成功');
      fetchDocuments();
      navigate(`/documents/${doc.id}`);
    } catch (error: any) {
      message.error(error.response?.data?.message || '导入失败');
    }
  };


  const handleCreateDocument = async (values: any) => {
    try {
      const doc = await documentApi.create({
        ...values,
        folderId: selectedFolderId,
      });
      message.success('文档创建成功');
      setCreateDocModalOpen(false);
      form.resetFields();
      navigate(`/documents/${doc.id}`);
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建失败');
    }
  };

  const handleCreateFolder = async (values: any) => {
    try {
      await folderApi.create({
        name: values.name,
        parentId: selectedFolderId,
      });
      message.success('文件夹创建成功');
      setCreateFolderModalOpen(false);
      folderForm.resetFields();
      fetchFolders();
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建失败');
    }
  };

  const handleDeleteFolder = async (folderId: number) => {
    const currentFolder = currentPath[currentPath.length - 1];
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除文件夹"${currentFolder.name}"吗？文件夹内的所有文档也将被删除。此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await folderApi.delete(folderId);
          message.success('删除成功');
          // 返回上一级
          const parentPath = currentPath.slice(0, -1);
          // 确保至少有根目录
          const finalPath = parentPath.length > 0 ? parentPath : [{ id: null, name: '根目录' }];
          const parentId = finalPath[finalPath.length - 1]?.id || null;
          setSelectedFolderId(parentId);
          setCurrentPath(finalPath);
          fetchFolders();
          // fetchDocuments() 会由 useEffect 自动触发，不需要手动调用
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  const handleDeleteDocument = async (doc: Document) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除文档"${doc.title}"吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await documentApi.delete(doc.id);
          message.success('删除成功');
          fetchDocuments();
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  const handleCloneDocument = async (doc: Document) => {
    try {
      const newDoc = await documentApi.clone(doc.id, { title: `${doc.title} - 副本` });
      message.success('克隆成功');
      navigate(`/documents/${newDoc.id}`);
    } catch (error: any) {
      message.error(error.response?.data?.message || '克隆失败');
    }
  };

  const handleGenerateInviteLink = async () => {
    if (!selectedDoc) return;
    try {
      const result = await collaboratorApi.createInviteLink(selectedDoc.id);
      Modal.success({
        title: '邀请链接已生成',
        content: (
          <div>
            <p>分享以下链接邀请他人协作：</p>
            <Input.TextArea
              value={result.inviteUrl}
              readOnly
              autoSize={{ minRows: 2 }}
            />
          </div>
        ),
      });
    } catch (error: any) {
      message.error(error.response?.data?.message || '生成失败');
    }
  };

  // 构建文件夹路径
  const buildFolderPath = (folderId: number | null, allFolders: Folder[]): { id: number | null; name: string }[] => {
    if (folderId === null) {
      return [{ id: null, name: '根目录' }];
    }

    const path: { id: number | null; name: string }[] = [];
    const findPath = (folders: Folder[], targetId: number): boolean => {
      for (const folder of folders) {
        if (folder.id === targetId) {
          path.push({ id: folder.id, name: folder.name });
          return true;
        }
        if (folder.children && folder.children.length > 0) {
          if (findPath(folder.children, targetId)) {
            path.unshift({ id: folder.id, name: folder.name });
            return true;
          }
        }
      }
      return false;
    };

    const found = findPath(allFolders, folderId);
    const result = found ? [{ id: null, name: '根目录' }, ...path] : [{ id: null, name: '根目录' }];
    // 去重：移除连续重复的节点，避免出现“根目录 > 根目录”
    const normalized = result.filter((item, idx, arr) => {
      if (idx === 0) {
        return true;
      }
      const prev = arr[idx - 1];
      return item.id !== prev.id || item.name !== prev.name;
    });
    return normalized;
  };

  const mapFolderToTree = (folder: Folder): DataNode => ({
    key: folder.id,
    title: folder.name,
    icon: <FolderOutlined />,
    children: (folder.children || []).map(mapFolderToTree),
  });

  const treeData: DataNode[] = folders.map(mapFolderToTree);

  const getDocumentMenuItems = (doc: Document) => [
    {
      key: 'edit',
      icon: <EditOutlined />,
      label: '编辑',
      onClick: () => navigate(`/documents/${doc.id}`),
    },
    {
      key: 'clone',
      icon: <CopyOutlined />,
      label: '克隆',
      onClick: () => handleCloneDocument(doc),
    },
    {
      key: 'share',
      icon: <ShareAltOutlined />,
      label: '分享',
      onClick: () => {
        setSelectedDoc(doc);
        setShareModalOpen(true);
      },
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '删除',
      danger: true,
      onClick: () => handleDeleteDocument(doc),
    },
  ];

  return (
    <div className="documents-page">
      <Row gutter={24}>
        {/* Folder Tree */}
        <Col span={6}>
          <Card
            title={
              <span
                style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
                onClick={() => {
                  setSelectedFolderId(null);
                  setCurrentPath([{ id: null, name: '根目录' }]);
                }}
              >
                <HomeOutlined />
                根目录
              </span>
            }
            size="small"
            extra={
              <Tooltip title="新建文件夹">
                <Button
                  type="text"
                  icon={<FolderAddOutlined />}
                  onClick={() => setCreateFolderModalOpen(true)}
                />
              </Tooltip>
            }
          >
            <Tree
              showIcon
              defaultExpandAll
              treeData={treeData}
              selectedKeys={selectedFolderId ? [String(selectedFolderId)] : []}
              onSelect={(keys) => {
                const key = keys[0] as string;
                if (!key) {
                  setSelectedFolderId(null);
                  setCurrentPath([{ id: null, name: '根目录' }]);
                } else {
                  const folderId = parseInt(key, 10);
                  if (!isNaN(folderId)) {
                    setSelectedFolderId(folderId);
                    setCurrentPath(buildFolderPath(folderId, folders));
                  } else {
                    setSelectedFolderId(null);
                    setCurrentPath([{ id: null, name: '根目录' }]);
                  }
                }
              }}
            />
          </Card>
        </Col>

        {/* Document List */}
        <Col span={18}>
          <Card className="documents-container">
            {/* 搜索和操作栏 */}
            <div className="documents-header">
              <Space>
                <Search
                  placeholder="搜索文档"
                  allowClear
                  onSearch={setSearchKeyword}
                  style={{ width: 240 }}
                />
              </Space>
              <Space>
                <Button
                  onClick={() => fileInputRef.current?.click()}
                >
                  导入文档
                </Button>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setCreateDocModalOpen(true)}
                >
                  新建文档
                </Button>
              </Space>
            </div>

            {/* 面包屑导航 - 当前路径 */}
            <div className="breadcrumb-container">
              <div className="breadcrumb-wrapper">
                <Breadcrumb
                  separator={<RightOutlined style={{ fontSize: 10 }} />}
                  items={currentPath.map((item, index) => ({
                    key: item.id ?? 'root',
                    title: (
                      <span
                        className={index === currentPath.length - 1 ? 'breadcrumb-current' : 'breadcrumb-link'}
                        onClick={() => {
                          if (index < currentPath.length - 1) {
                            setSelectedFolderId(item.id);
                            setCurrentPath(currentPath.slice(0, index + 1));
                          }
                        }}
                        style={{
                          cursor: index === currentPath.length - 1 ? 'default' : 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 4,
                        }}
                      >
                        {index === 0 && <HomeOutlined />}
                        {item.name}
                      </span>
                    ),
                  }))}
                />
                <Space className="folder-actions">
                  <Tooltip title="在当前位置新建文件夹">
                    <Button
                      size="small"
                      icon={<FolderAddOutlined />}
                      onClick={() => setCreateFolderModalOpen(true)}
                    >
                      新建文件夹
                    </Button>
                  </Tooltip>
                  {selectedFolderId !== null && (
                    <Tooltip title="删除当前文件夹">
                      <Button
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDeleteFolder(selectedFolderId)}
                      >
                        删除文件夹
                      </Button>
                    </Tooltip>
                  )}
                </Space>
              </div>
            </div>

            <Divider style={{ margin: '12px 0' }} />

            {/* 文档显示区域 */}
            <div className="documents-display-area">

          <input
            ref={fileInputRef}
            type="file"
            accept=".docx,.md,.markdown,.txt"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                handleImportDocument(file);
              }
              e.target.value = '';
            }}
          />

          {!documents || documents.length === 0 ? (
            <Empty
              description="暂无文档"
              style={{ marginTop: 100 }}
            >
              <Button type="primary" onClick={() => setCreateDocModalOpen(true)}>
                创建第一个文档
              </Button>
            </Empty>
          ) : (
            <List
              loading={loading}
              dataSource={documents}
              renderItem={(doc) => (
                <Card
                  className="document-card"
                  hoverable
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <div className="document-card-content">
                    <div className="document-info">
                      <div className="document-title">
                        <FileTextOutlined className="document-icon" />
                        <span>{doc.title}</span>
                        <Tag color="cyan">{doc.docType?.toUpperCase() || 'MARKDOWN'}</Tag>
                        {doc.visibility.toLowerCase() === 'private' ? (
                          <Tag icon={<LockOutlined />} color="default">私有</Tag>
                        ) : (
                          <Tag icon={<GlobalOutlined />} color="blue">公开</Tag>
                        )}
                      </div>
                      <div className="document-meta">
                        <span>更新于 {dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm')}</span>
                        {doc.owner && (
                          <span className="owner">创建者: {doc.owner.username}</span>
                        )}
                      </div>
                    </div>
                    <Dropdown
                      menu={{ items: getDocumentMenuItems(doc) }}
                      trigger={['click']}
                    >
                      <Button
                        type="text"
                        icon={<MoreOutlined />}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </Dropdown>
                  </div>
                </Card>
              )}
            />
          )}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Create Document Modal */}
      <Modal
        title="新建文档"
        open={createDocModalOpen}
        onCancel={() => {
          setCreateDocModalOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCreateDocument}>
          <Form.Item
            name="title"
            label="文档标题"
            rules={[{ required: true, message: '请输入文档标题' }]}
          >
            <Input placeholder="请输入文档标题" />
          </Form.Item>
          <Form.Item name="docType" label="文档类型" initialValue="markdown">
            <Select>
              <Select.Option value="markdown">Markdown</Select.Option>
              <Select.Option value="txt">纯文本 (TXT)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="visibility" label="可见性" initialValue="private">
            <Select>
              <Select.Option value="private">私有</Select.Option>
              <Select.Option value="public">公开</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Create Folder Modal */}
      <Modal
        title="新建文件夹"
        open={createFolderModalOpen}
        onCancel={() => {
          setCreateFolderModalOpen(false);
          folderForm.resetFields();
        }}
        onOk={() => folderForm.submit()}
      >
        <Form form={folderForm} layout="vertical" onFinish={handleCreateFolder}>
          <Form.Item
            name="name"
            label="文件夹名称"
            rules={[{ required: true, message: '请输入文件夹名称' }]}
          >
            <Input placeholder="请输入文件夹名称" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Share Modal */}
      <Modal
        title="分享文档"
        open={shareModalOpen}
        onCancel={() => setShareModalOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setShareModalOpen(false)}>
            关闭
          </Button>,
          <Button key="generate" type="primary" onClick={handleGenerateInviteLink}>
            生成邀请链接
          </Button>,
        ]}
      >
        <p>生成邀请链接后，其他用户可以通过链接加入协作。</p>
      </Modal>
    </div>
  );
};

export default Documents;
