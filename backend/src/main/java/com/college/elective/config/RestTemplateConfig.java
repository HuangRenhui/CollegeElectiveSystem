package com.college.elective.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 外部 HTTP 调用配置。
 *
 * <p>目前用于调用微信小程序接口。之所以要显式创建 Bean 而不是直接
 * {@code new RestTemplate()}，是为了<b>设置超时时间</b>：</p>
 *
 * <p>默认的 {@code RestTemplate} 没有连接与读取超时，
 * 一旦微信接口响应缓慢，调用线程会被无限期挂起，
 * 在选课/登录等高峰期会迅速耗尽 Tomcat 线程池并拖垮整个服务。</p>
 */
@Configuration
public class RestTemplateConfig {

    /** 建立连接的超时时间 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /** 等待响应的超时时间 */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
