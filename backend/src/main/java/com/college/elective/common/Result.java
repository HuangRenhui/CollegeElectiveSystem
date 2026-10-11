package com.college.elective.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应体。
 *
 * @param <T> 业务数据类型
 */
@Data
@Accessors(chain = true)
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码，200 表示成功")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "响应时间")
    private LocalDateTime timestamp;

    /**
     * 响应时刻的 epoch 毫秒，作为前端的时间基准。
     *
     * <p>前端据此计算「服务器时刻 - 本地时刻」的偏移量，从而让选课倒计时
     * 不受用户本机时钟被修改的影响。选课窗口的最终判定仍以服务端为准，
     * 此字段只用于保证前端展示与按钮状态的一致性。</p>
     */
    @Schema(description = "服务器时间戳（epoch 毫秒），供前端校正本地时钟")
    private Long serverTime;

    @Schema(description = "是否成功")
    private Boolean success;

    public Result() {
        this.timestamp = LocalDateTime.now();
        this.serverTime = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(ResultCode.FAIL.getCode(), message);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        result.setSuccess(false);
        return result;
    }
}
