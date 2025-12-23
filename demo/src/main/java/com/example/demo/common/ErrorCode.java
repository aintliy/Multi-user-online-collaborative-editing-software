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
    VERSION_NOT_FOUND(3004, "版本不存在"),
    COLLABORATOR_NOT_FOUND(3005, "协作者不存在"),
    COLLABORATOR_ALREADY_EXISTS(3006, "协作者已存在"),
    CANNOT_ADD_SELF(3007, "不能添加自己为协作者"),
    
    // 评论和通知相关 3500-3599
    COMMENT_NOT_FOUND(3501, "评论不存在"),
    COMMENT_NO_PERMISSION(3502, "无权操作该评论"),
    NOTIFICATION_NOT_FOUND(3510, "通知不存在"),
    NOTIFICATION_NO_PERMISSION(3511, "无权操作该通知"),
    
    // 任务相关 3600-3699
    TASK_NOT_FOUND(3601, "任务不存在"),
    TASK_NO_PERMISSION(3602, "无权操作该任务"),
    
    // 好友相关 3650-3669
    CANNOT_ADD_SELF_AS_FRIEND(3650, "不能添加自己为好友"),
    ALREADY_FRIENDS(3651, "已经是好友"),
    FRIEND_REQUEST_ALREADY_SENT(3652, "好友请求已发送"),
    FRIEND_REQUEST_NOT_FOUND(3653, "好友请求不存在"),
    FRIEND_REQUEST_NO_PERMISSION(3654, "无权操作该好友请求"),
    FRIEND_REQUEST_ALREADY_PROCESSED(3655, "好友请求已处理"),
    
    // 管理员相关 3670-3689
    CANNOT_UPDATE_SELF_ROLE(3670, "不能修改自己的角色"),
    CANNOT_UPDATE_SELF_STATUS(3671, "不能修改自己的状态"),
    CANNOT_DELETE_SELF(3672, "不能删除自己"),
    LAST_ADMIN_REQUIRED(3673, "系统至少保留一名活跃管理员"),
    
    // 文件相关 3690-3699
    FILE_EMPTY(3690, "文件为空"),
    FILE_NAME_INVALID(3691, "文件名无效"),
    FILE_TYPE_INVALID(3692, "文件类型不支持"),
    
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
