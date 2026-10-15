package com.college.elective.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。用于操作日志落库等非核心链路任务。
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean("operationLogExecutor")
    public ThreadPoolTaskExecutor operationLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(2000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("elective-log-");
        // 队列满时由调用线程执行，保证日志不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 邮件通知专用线程池。
     *
     * <p><b>为什么不复用 {@code operationLogExecutor}</b>：两者的可靠性要求相反——</p>
     * <ul>
     *   <li>操作日志是<b>审计依据，不能丢</b>，故队列极大且用
     *       {@code CallerRunsPolicy}（宁可拖慢请求也要写入）；</li>
     *   <li>邮件是<b>尽力送达的提醒</b>，且 SMTP 握手有秒级延迟。
     *       若共用线程池，一次 SMTP 卡顿会连带把日志写入也拖住。</li>
     * </ul>
     *
     * <p>因此这里用较小的独立池，并保留默认的 {@code AbortPolicy}：
     * 队列满时直接抛 {@code TaskRejectedException} 由调用方捕获记录，
     * 而<b>不是</b>让选课/成绩发布的用户请求去等 SMTP 返回。</p>
     */
    @Bean("mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("elective-mail-");
        // 不使用 CallerRunsPolicy：那会让用户请求阻塞在 SMTP 上
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 比日志短：邮件是提醒，关闭时不必久等
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return operationLogExecutor();
    }
}
