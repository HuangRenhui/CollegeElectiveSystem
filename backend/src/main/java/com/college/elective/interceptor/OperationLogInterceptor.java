package com.college.elective.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求耗时统计拦截器。
 *
 * <p>用于在 DEBUG 日志中观察接口性能，重点监控选课等高频接口。</p>
 */
@Slf4j
@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "REQUEST_START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_TIME_ATTR);
        if (start == null) {
            return;
        }
        long cost = System.currentTimeMillis() - (Long) start;
        if (log.isDebugEnabled()) {
            log.debug("{} {} 耗时 {} ms，状态码 {}", request.getMethod(), request.getRequestURI(),
                    cost, response.getStatus());
        }
        if (cost > 1000) {
            log.warn("接口响应较慢: {} {} 耗时 {} ms", request.getMethod(), request.getRequestURI(), cost);
        }
    }
}
