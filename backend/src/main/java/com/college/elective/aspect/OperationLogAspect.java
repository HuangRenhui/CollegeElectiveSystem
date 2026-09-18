package com.college.elective.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.college.elective.common.IpUtils;
import com.college.elective.entity.SysLog;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * 操作日志切面：异步落库，避免影响主流程性能。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService sysLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            errorMsg = StrUtil.maxLength(throwable.getMessage(), 900);
            throw throwable;
        } finally {
            try {
                saveLog(joinPoint, operationLog, System.currentTimeMillis() - start, success, errorMsg);
            } catch (Exception e) {
                log.warn("记录操作日志失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         long costTime, boolean success, String errorMsg) {
        HttpServletRequest request = currentRequest();
        SysLog sysLog = new SysLog();
        sysLog.setModule(operationLog.module());
        sysLog.setOperation(operationLog.operation());
        sysLog.setCostTime(costTime);
        sysLog.setSuccess(success ? 1 : 0);
        sysLog.setErrorMsg(errorMsg);
        sysLog.setCreateTime(java.time.LocalDateTime.now());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        sysLog.setMethod(method.getDeclaringClass().getName() + "#" + method.getName());

        if (request != null) {
            sysLog.setRequestUri(request.getRequestURI());
            sysLog.setIp(IpUtils.resolveIp(request));
        }

        Optional<LoginUser> loginUser = SecurityUtils.getLoginUserOptional();
        loginUser.ifPresent(user -> {
            sysLog.setUserId(user.getUserId());
            sysLog.setUsername(user.getUsername());
        });

        if (operationLog.saveParam()) {
            sysLog.setRequestParam(resolveParams(joinPoint.getArgs()));
        }

        sysLogService.saveAsync(sysLog);
    }

    private String resolveParams(Object[] args) {
        try {
            Object[] printable = Arrays.stream(args)
                    .map(arg -> arg instanceof MultipartFile file
                            ? "MultipartFile(" + file.getOriginalFilename() + ")"
                            : arg)
                    .toArray();
            String json = JSONUtil.toJsonStr(printable);
            return StrUtil.maxLength(json, 2000);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
