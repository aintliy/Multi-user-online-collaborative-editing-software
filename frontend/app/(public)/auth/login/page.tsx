import { Suspense } from "react";
import LoginForm from "./LoginForm";

const LoginPage = () => {
  return (
    <Suspense fallback={<div style={{ minHeight: 240 }} />}>
      <LoginForm />
    </Suspense>
  );
};

export default LoginPage;
