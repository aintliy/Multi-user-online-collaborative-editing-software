"use client";

import { Button, ButtonProps, message } from "antd";
import { useState } from "react";
import { useCountdown } from "@/hooks/useCountdown";

interface CountdownButtonProps extends ButtonProps {
  seconds?: number;
  action: () => Promise<void>;
}

const CountdownButton = ({ seconds = 60, action, children, ...buttonProps }: CountdownButtonProps) => {
  const { seconds: left, start, running } = useCountdown(seconds);
  const [loading, setLoading] = useState(false);

  const handleClick = async () => {
    try {
      setLoading(true);
      await action();
      start();
      message.success("验证码已发送");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Button {...buttonProps} loading={loading} disabled={running} onClick={handleClick}>
      {running ? `${left}s 后重试` : children}
    </Button>
  );
};

export default CountdownButton;
