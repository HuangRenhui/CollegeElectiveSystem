package com.college.elective.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。保证所有异常都以统一结构返回给前端。
 *
 * <p><b>关于 HTTP 状态码</b>：响应体始终是统一的 {@code Result} 结构，
 * 但 HTTP 状态码必须与错误性质一致——否则网关、APM、告警规则与客户端重试策略
 * 看到的永远是 200，鉴权失败与系统崩溃在监控上完全不可见（这正是本类此前的缺陷）。</p>
 *
 * <p>映射原则：<b>只把「与 HTTP 语义同名」的通用错误码映射为对应状态</b>
 * （400/401/403/404/405/5xx），业务错误码（2001~7006）仍返回 200，
 * 以免改变「业务失败 ≠ 请求失败」这一既有前端契约。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request,
                                                HttpServletResponse response) {
        log.warn("业务异常 [{}] {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return fail(response, e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(BindException e, HttpServletResponse response) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return fail(response, ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletResponse response) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return fail(response, ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e, HttpServletResponse response) {
        return fail(response, ResultCode.PARAM_ERROR, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        return fail(response, ResultCode.PARAM_ERROR, "参数类型不正确：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e, HttpServletResponse response) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return fail(response, ResultCode.PARAM_ERROR, "请求体格式不正确，请检查提交的数据");
    }

    /**
     * 上传体积超限。
     *
     * <p>未单独处理时会落到兜底分支返回「系统异常」，
     * 用户完全看不出是「文件太大」，只能反复重试。</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException e, HttpServletResponse response) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return fail(response, ResultCode.PARAM_ERROR, "上传文件超过大小限制，请压缩或拆分后重试");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                 HttpServletResponse response) {
        return fail(response, ResultCode.METHOD_NOT_ALLOWED, "不支持的请求方法：" + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                    HttpServletResponse response) {
        return fail(response, ResultCode.PARAM_ERROR, "请求的 Content-Type 不受支持，请使用 application/json");
    }

    /**
     * 静态资源/未匹配路径。
     *
     * <p>Spring Boot 3.2+ 对未命中任何 handler 的请求抛 {@link NoResourceFoundException}，
     * 若不单独处理，会被兜底分支当成「系统异常」记录成 error 日志并返回 500 文案，
     * 既污染日志又误导排查。</p>
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Result<Void> handleNoHandlerFound(Exception e, HttpServletRequest request,
                                             HttpServletResponse response) {
        log.debug("接口不存在 [{}] {}", request.getMethod(), request.getRequestURI());
        return fail(response, ResultCode.NOT_FOUND, "接口不存在：" + request.getRequestURI());
    }

    /**
     * 认证失败。
     *
     * <p><b>不回传 {@code e.getMessage()}</b>：认证异常文本可能包含内部实现细节
     * （如凭据校验失败原因、令牌解析栈信息），对匿名请求方没有价值，只会造成信息泄露。</p>
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletResponse response) {
        log.warn("认证失败: {}", e.getMessage());
        return fail(response, ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletResponse response) {
        return fail(response, ResultCode.FORBIDDEN);
    }

    /**
     * 异步请求超时（如排队轮询、长轮询）。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public Result<Void> handleAsyncTimeout(AsyncRequestTimeoutException e, HttpServletResponse response) {
        log.warn("异步请求处理超时: {}", e.getMessage());
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        return Result.error(ResultCode.SYSTEM_BUSY);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e, HttpServletResponse response) {
        log.warn("唯一索引冲突: {}", e.getMessage());
        return fail(response, ResultCode.DATA_ALREADY_EXISTS);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e,
                                                     HttpServletResponse response) {
        log.warn("数据完整性校验失败: {}", e.getMessage());
        return fail(response, ResultCode.FAIL, "数据存在关联或字段不合法，操作失败");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("系统异常 [{}] {}", request.getMethod(), request.getRequestURI(), e);
        return fail(response, ResultCode.FAIL, "系统异常，请联系管理员");
    }

    // ---------------------------- 内部工具 ----------------------------

    private Result<Void> fail(HttpServletResponse response, ResultCode code) {
        return fail(response, code.getCode(), code.getMessage());
    }

    private Result<Void> fail(HttpServletResponse response, ResultCode code, String message) {
        return fail(response, code.getCode(), message);
    }

    /**
     * 按错误码设置 HTTP 状态并组装响应体。
     *
     * @param response 响应对象
     * @param code     业务错误码，可能为 {@code null}
     * @param message  提示信息
     * @return 统一响应体
     */
    private Result<Void> fail(HttpServletResponse response, Integer code, String message) {
        response.setStatus(toHttpStatus(code));
        return Result.error(code, message);
    }

    /**
     * 业务错误码 → HTTP 状态码。
     *
     * <p>只映射与 HTTP 语义一致的通用码，其余（业务码）返回 200，
     * 保持「业务失败仍是成功响应」的既有契约，避免前端把所有业务提示都当成网络错误。</p>
     */
    private int toHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.OK.value();
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST.value();
            case 401, 2003 -> HttpStatus.UNAUTHORIZED.value();
            case 403, 2007 -> HttpStatus.FORBIDDEN.value();
            case 404 -> HttpStatus.NOT_FOUND.value();
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED.value();
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR.value();
            default -> HttpStatus.OK.value();
        };
    }
}
