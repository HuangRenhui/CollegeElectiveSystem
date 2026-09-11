package com.college.elective.common;

import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。由 {@link GlobalExceptionHandler} 统一捕获并转换为 {@link Result}。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 条件成立时抛出业务异常。
     */
    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 条件成立时抛出业务异常并自定义提示。
     */
    public static void throwIf(boolean condition, ResultCode resultCode, String message) {
        if (condition) {
            throw new BusinessException(resultCode, message);
        }
    }
}
