package com.college.elective.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统自定义配置项，对应 application.yml 中的 {@code elective.*}。
 */
@Data
@ConfigurationProperties(prefix = "elective")
public class ElectiveProperties {

    private Jwt jwt = new Jwt();

    private Selection selection = new Selection();

    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        /** 签名密钥，长度至少 32 字节 */
        private String secret = "CollegeElectiveSystemSecretKeyForJwtTokenMustBeLongEnough2026";
        /** 令牌有效期（秒） */
        private long expireSeconds = 7200L;
        /** 剩余有效期低于该值时自动续期（秒） */
        private long refreshThresholdSeconds = 600L;
        /** 令牌所在请求头 */
        private String header = "Authorization";
        /** 令牌前缀 */
        private String prefix = "Bearer ";
    }

    @Data
    public static class Selection {
        /** 单个学生可选学分上限 */
        private double maxCredit = 30.0;
        /** 分布式锁等待时间（毫秒） */
        private long lockWaitMillis = 300L;
        /** 是否开放选课 */
        private boolean enabled = true;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5174"));
    }
}
