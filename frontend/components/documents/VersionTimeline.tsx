"use client";

import { Timeline, Button } from "antd";
import dayjs from "dayjs";
import { DocumentVersion } from "@/types";

interface VersionTimelineProps {
  versions: DocumentVersion[];
  onRollback: (versionNo: number) => void;
}

const VersionTimeline = ({ versions, onRollback }: VersionTimelineProps) => {
  return (
    <Timeline
      items={versions.map((version) => ({
        color: "#155eef",
        children: (
          <div>
            <strong>v{version.versionNo}</strong> - {version.creatorName}
            <div style={{ color: "var(--text-muted)", fontSize: 12 }}>
              {dayjs(version.createdAt).format("YYYY-MM-DD HH:mm")}
            </div>
            <div style={{ margin: "8px 0" }}>{version.commitMessage}</div>
            <Button size="small" onClick={() => onRollback(version.versionNo)}>
              回滚到该版本
            </Button>
          </div>
        ),
      }))}
    />
  );
};

export default VersionTimeline;
