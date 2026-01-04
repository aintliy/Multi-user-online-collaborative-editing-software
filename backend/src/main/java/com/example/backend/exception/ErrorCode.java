package com.example.backend.exception;

/**
 * 错误码常量
 */
public class ErrorCode {
    
    // 通用错误码
    public static final int SUCCESS = 0;
    public static final int UNKNOWN_ERROR = 1000;
    public static final int PARAM_ERROR = 1001;
    public static final int UNAUTHORIZED = 1002;
    public static final int FORBIDDEN = 1003;
    public static final int TOO_MANY_REQUESTS = 1004;
    public static final int ACCESS_DENIED = 1005;
    public static final int SYSTEM_ERROR = 1006;
    public static final int FILE_NOT_FOUND = 1007;
    
    // 用户相关错误码 2xxx
    public static final int USER_NOT_FOUND = 2001;
    public static final int USER_ALREADY_EXISTS = 2002;
    public static final int PASSWORD_ERROR = 2003;
    public static final int VERIFICATION_CODE_ERROR = 2004;
    public static final int TOKEN_INVALID = 2005;
    public static final int USER_DISABLED = 2006;
    
    // 文档相关错误码 3xxx
    public static final int DOCUMENT_NOT_FOUND = 3001;
    public static final int FOLDER_NOT_FOUND = 3002;
    public static final int FOLDER_NAME_DUPLICATE = 3003;
    public static final int DOCUMENT_ACCESS_DENIED = 3004;
    public static final int VERSION_NOT_FOUND = 3005;
    public static final int INVALID_OPERATION = 3006;
    
    // 协作相关错误码 4xxx
    public static final int COLLABORATOR_ALREADY_EXISTS = 4001;
    public static final int COLLABORATOR_NOT_FOUND = 4002;
    public static final int INVITE_LINK_INVALID = 4003;
    public static final int WORKSPACE_REQUEST_EXISTS = 4004;
    public static final int WORKSPACE_REQUEST_NOT_FOUND = 4005;
    
    // 好友相关错误码 5xxx
    public static final int FRIEND_REQUEST_EXISTS = 5001;
    public static final int FRIEND_REQUEST_NOT_FOUND = 5002;
    public static final int ALREADY_FRIENDS = 5003;
    public static final int CANNOT_ADD_SELF = 5004;
}
