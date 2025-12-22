package com.example.demo.common;

/**
 * 错误码枚举
 * 按照接口设计文档规范定义
 */
public enum ErrorCode {

    // 成功
    SUCCESS(0, "OK"),

    // 通用错误 1000-1999
    PARAM_ERROR(1001, "参数校验失败"),
    AUTH_ERROR(1002, "认证失败（未登录或 token 失效）"),
    FORBIDDEN(1003, "权限不足"),
    TOO_MANY_REQUESTS(1004, "访问过于频繁，请稍后重试"),

    // 用户相关 2000-2999
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "邮箱/手机号已被注册"),
    USER_PASSWORD_ERROR(2003, "密码错误"),
    USER_DISABLED(2004, "账号已被禁用"),
    INVALID_TOKEN(2005, "令牌无效或已过期"),
    VERIFICATION_CODE_ERROR(2006, "验证码错误或已过期"),

    // 文档相关 3000-3999
    DOCUMENT_NOT_FOUND(3001, "文档不存在"),
    DOCUMENT_NO_PERMISSION(3002, "无权访问该文档"),
    DOCUMENT_VERSION_NOT_FOUND(3003, "文档版本不存在"),
    
    // 评论和通知相关 3500-3599
    COMMENT_NOT_FOUND(3501, "评论不存在"),
    NOTIFICATION_NOT_FOUND(3502, "通知不存在"),
    
    // 任务相关 3600-3699
    TASK_NOT_FOUND(3601, "任务不存在"),
    
    // 角色权限相关 3700-3799
    ROLE_NOT_FOUND(3701, "角色不存在"),
    PERMISSION_NOT_FOUND(3702, "权限不存在"),

    // WebSocket 相关 4000-4999
    WS_CONNECTION_ERROR(4001, "WebSocket 连接非法"),
    WS_AUTH_ERROR(4002, "WebSocket 认证失败"),

    // 系统错误 5000+
    SYSTEM_ERROR(5000, "系统内部错误"),
    DATABASE_ERROR(5001, "数据库错误"),
    NETWORK_ERROR(5002, "网络错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
