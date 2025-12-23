"use client";

import { Card, Col, Row, Tag, Typography, Button, Space } from "antd";
import { Task, TaskStatus } from "@/types";
import dayjs from "dayjs";

interface TaskBoardProps {
  tasks: Task[];
  onView: (task: Task) => void;
  onStatusChange: (task: Task, status: TaskStatus) => void;
}

const statusMeta: Record<TaskStatus, { title: string; color: string }> = {
  TODO: { title: "待处理", color: "#94a3b8" },
  DOING: { title: "进行中", color: "#f59e0b" },
  DONE: { title: "已完成", color: "#22c55e" },
};

const TaskBoard = ({ tasks, onView, onStatusChange }: TaskBoardProps) => {
  return (
    <Row gutter={16}>
      {(Object.keys(statusMeta) as TaskStatus[]).map((status) => (
        <Col span={8} key={status}>
          <Card
            title={statusMeta[status].title}
            bordered={false}
            style={{ borderRadius: 16, background: "#fff" }}
            headStyle={{ borderBottom: "none", color: statusMeta[status].color, fontWeight: 600 }}
          >
            <Space direction="vertical" style={{ width: "100%" }}>
              {tasks.filter((task) => task.status === status).map((task) => (
                <Card key={task.id} size="small" bordered style={{ borderRadius: 12 }}>
                  <Typography.Text strong>{task.title}</Typography.Text>
                  <div style={{ color: "var(--text-muted)", fontSize: 12 }}>
                    {task.documentTitle}
                  </div>
                  <Space size={8} style={{ marginTop: 8 }}>
                    <Tag color={task.priority === "HIGH" ? "red" : task.priority === "MEDIUM" ? "orange" : "blue"}>
                      {task.priority}
                    </Tag>
                    {task.dueDate && <Tag>{dayjs(task.dueDate).format("MM-DD")}</Tag>}
                  </Space>
                  <Space style={{ marginTop: 8 }}>
                    <Button type="link" size="small" onClick={() => onView(task)}>
                      查看
                    </Button>
                    {status !== "DONE" && (
                      <Button type="link" size="small" onClick={() => onStatusChange(task, status === "TODO" ? "DOING" : "DONE")}>
                        {status === "TODO" ? "开始" : "完成"}
                      </Button>
                    )}
                  </Space>
                </Card>
              ))}
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  );
};

export default TaskBoard;
