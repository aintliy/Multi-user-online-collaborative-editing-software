import { Suspense } from "react";
import ResetPasswordForm from "./ResetPasswordForm";

const ResetPasswordPage = () => {
  return (
    <Suspense fallback={<div style={{ minHeight: 240 }} />}>
      <ResetPasswordForm />
    </Suspense>
  );
};

export default ResetPasswordPage;
