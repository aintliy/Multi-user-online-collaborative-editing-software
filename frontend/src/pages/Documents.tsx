import React, { useEffect, useState } from 'react';
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
  
  // Modals
  const [createDocModalOpen, setCreateDocModalOpen] = useState(false);
  const [createFolderModalOpen, setCreateFolderModalOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  
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
      const data = await documentApi.getList({
        folderId: selectedFolderId ?? undefined,
        keyword: searchKeyword || undefined,
      });
      setDocuments(data.content);
    } catch (error) {
      console.error('Failed to fetch documents:', error);
    } finally {
      setLoading(false);
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

  // Build tree data for folders
  const buildTreeData = (folders: Folder[], parentId: number | null = null): DataNode[] => {
    return folders
      .filter(f => f.parentId === parentId)
      .map(folder => ({
        key: folder.id,
        title: folder.name,
        icon: <FolderOutlined />,
        children: buildTreeData(folders, folder.id),
      }));
  };

  const treeData: DataNode[] = [
    {
      key: 'all',
      title: '全部文档',
      icon: <FileTextOutlined />,
    },
    ...buildTreeData(folders),
  ];

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
            title="文件夹"
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
              selectedKeys={selectedFolderId ? [selectedFolderId] : ['all']}
              onSelect={(keys) => {
                const key = keys[0];
                setSelectedFolderId(key === 'all' ? null : Number(key));
              }}
            />
          </Card>
        </Col>

        {/* Document List */}
        <Col span={18}>
          <div className="documents-header">
            <Space>
              <Search
                placeholder="搜索文档"
                allowClear
                onSearch={setSearchKeyword}
                style={{ width: 240 }}
              />
            </Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateDocModalOpen(true)}
            >
              新建文档
            </Button>
          </div>

          {documents.length === 0 ? (
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
                        {doc.visibility === 'PRIVATE' ? (
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
          <Form.Item name="visibility" label="可见性" initialValue="PRIVATE">
            <Select>
              <Select.Option value="PRIVATE">私有</Select.Option>
              <Select.Option value="PUBLIC">公开</Select.Option>
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
