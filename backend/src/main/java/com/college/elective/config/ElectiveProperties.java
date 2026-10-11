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
        /**
         * 是否启用选课排队削峰。
         *
         * <p>关闭时选课链路与原先完全一致（无任何额外开销），
         * 便于在非高峰期或压测环境中关闭。</p>
         */
        private boolean queueEnabled = false;
        /**
         * 每秒放行名额，即选课高峰期的最大并发准入速率。
         *
         * <p>取值建议：不超过数据库连接池容量的 1/2，
         * 避免排队放行速度本身就把连接池打满。</p>
         */
        private int queuePerSecond = 50;
        /** 单课程最大排队人数，超出直接拒绝，防止无限堆积 */
        private int queueMaxSize = 5000;
        /**
         * 排队令牌有效期（秒）。
         *
         * <p>从取号开始计时，超时未完成选课则令牌失效、需重新取号。
         * 应略大于「排队等待 + 选课操作」的预期总耗时。</p>
         */
        private long queueTokenTtlSeconds = 120L;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5174"));
    }
}
