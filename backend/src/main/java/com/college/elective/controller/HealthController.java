package com.college.elective.controller;

import com.college.elective.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查端点。
 *
 * <p><b>为什么需要独立的 /health</b>：此前容器探活指向 {@code /v3/api-docs}
 * （Swagger 文档路径），而生产环境已通过 {@code springdoc.api-docs.enabled=false}
 * 关闭该接口，探活因此恒定 404 —— 容器永远处于 unhealthy，
 * 编排层、负载均衡与告警都会误判服务不可用。</p>
 *
 * <p>本端点只证明「进程能响应 HTTP」，不查数据库与 Redis：
 * 探活若依赖外部存储，一次数据库抖动会连带触发容器重启风暴，
 * 反而放大故障。依赖健康度应由监控系统单独采集。</p>
 */
@Tag(name = "系统")
@RestController
public class HealthController {

    /**
     * 存活探针。
     *
     * @return {@code UP} 表示进程可正常响应
     */
    @Operation(summary = "健康检查", description = "供容器/网关探活使用，无需认证")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("UP");
    }
}
