package com.example.demo.common;

/**
 * 业务异常类
 * 用于抛出可预期的业务异常
 */
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取ErrorCode对象（用于向后兼容）
     */
    public ErrorCode getErrorCode() {
        // 根据code查找对应的ErrorCode
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.getCode().equals(this.code)) {
                return ec;
            }
        }
        // 如果找不到，返回通用错误
        return ErrorCode.PARAM_ERROR;
    }
}
