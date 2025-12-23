"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, DatePicker, Form, Input, Select, message } from "antd";
import type { Dayjs } from "dayjs";
import PageHeader from "@/components/common/PageHeader";
import TaskBoard from "@/components/tasks/TaskBoard";
import { tasksService } from "@/services/tasks";
import { Task, TaskPriority, TaskStatus } from "@/types";

interface TaskFormValues {
  title: string;
  description?: string;
  documentId?: string;
  priority: TaskPriority;
  dueDate?: Dayjs;
}

const TasksPage = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [form] = Form.useForm();

  const fetchTasks = useCallback(async () => {
    const result = await tasksService.list({ page: 1, pageSize: 100 });
    setTasks(result.items);
  }, []);

  useEffect(() => {
    void (async () => {
      await fetchTasks();
    })();
  }, [fetchTasks]);

  const handleCreate = async (values: TaskFormValues) => {
    await tasksService.create({
      title: values.title,
      description: values.description,
      documentId: values.documentId ? Number(values.documentId) : undefined,
      priority: values.priority,
      dueDate: values.dueDate ? values.dueDate.format("YYYY-MM-DD") : undefined,
    });
    message.success("任务创建成功");
    form.resetFields();
    fetchTasks();
  };

  const handleStatusChange = async (task: Task, status: TaskStatus) => {
    await tasksService.update(task.id, { status });
    fetchTasks();
  };

  return (
    <div>
      <PageHeader title="我的任务" description="分配、跟踪与完成协作任务" />
      <div className="page-section">
        <Form form={form} layout="inline" onFinish={handleCreate} style={{ gap: 16 }}>
          <Form.Item name="title" rules={[{ required: true, message: "请输入任务标题" }]}> 
            <Input placeholder="任务标题" />
          </Form.Item>
          <Form.Item name="documentId">
            <Input placeholder="关联文档ID" />
          </Form.Item>
          <Form.Item name="priority" initialValue="MEDIUM">
            <Select style={{ width: 120 }} options={[{ label: "低", value: "LOW" }, { label: "中", value: "MEDIUM" }, { label: "高", value: "HIGH" }]} />
          </Form.Item>
          <Form.Item name="dueDate">
            <DatePicker format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              创建任务
            </Button>
          </Form.Item>
        </Form>
      </div>
      <div className="page-section">
        <TaskBoard tasks={tasks} onView={(task) => message.info(`任务：${task.title}`)} onStatusChange={handleStatusChange} />
      </div>
    </div>
  );
};

export default TasksPage;
