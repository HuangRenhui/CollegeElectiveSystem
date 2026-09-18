package com.college.elective.security;

import com.college.elective.common.RedisKeys;
import com.college.elective.config.ElectiveProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 令牌生成与解析工具。
 *
 * <p>除 JWT 自身签名校验外，令牌同时写入 Redis，支持服务端主动失效（退出登录、管理员禁用账号）。</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final ElectiveProperties properties;
    private final StringRedisTemplate stringRedisTemplate;

    private final SecretKey secretKey;
    //启动即失败：如果哪天有人误改了 elective.jwt.secret 配置，
    // 服务会立刻启动失败并告诉你原因，而不是等到生产环境第一个用户登录时才发现
    //（双重检测：登陆时有问题才爆发）
    public JwtTokenProvider(ElectiveProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.stringRedisTemplate = redisTemplate;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT 密钥长度不足 32 字节，当前为 " + secretBytes.length + " 字节，请在配置中调整 elective.jwt.secret");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * 生成访问令牌。
     *
     * @param userId   用户ID
     * @param username 登录账号
     * @param role     角色标识（STUDENT/TEACHER/ADMIN）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        long expireSeconds = properties.getJwt().getExpireSeconds();
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("uid", userId);
        claims.put("username", username);
        claims.put("role", role);

        String token = Jwts.builder()
            .claims(claims)
            .subject(String.valueOf(userId))
            .issuer("college-elective-system")
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expireSeconds * 1000))
            .signWith(secretKey)
            .compact();

        // 写入 Redis，作为令牌白名单，支持服务端主动注销
        stringRedisTemplate.opsForValue().set(
            RedisKeys.AUTH_TOKEN + userId, token, expireSeconds, TimeUnit.SECONDS);
        return token;
    }

    /**
     * 解析令牌，非法或过期返回 null。
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer("college-elective-system")
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验令牌是否仍在白名单内（未被注销）。
     */
    public boolean isTokenValid(Long userId, String token) {
        String cached = stringRedisTemplate.opsForValue().get(RedisKeys.AUTH_TOKEN + userId);
        return cached != null && cached.equals(token);
    }

    /**
     * 判断令牌剩余有效期是否低于续期阈值。
     */
    public boolean shouldRefresh(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return false;
        }
        long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return remainingSeconds > 0
            && remainingSeconds < properties.getJwt().getRefreshThresholdSeconds();
    }

    /**
     * 将令牌有效期延长至配置值（滑动过期）。
     */
    public void refreshToken(Long userId, String token) {
        long expireSeconds = properties.getJwt().getExpireSeconds();
        stringRedisTemplate.opsForValue().set(
            RedisKeys.AUTH_TOKEN + userId, token, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 注销令牌：从白名单移除。
     */
    public void invalidateToken(Long userId) {
        stringRedisTemplate.delete(RedisKeys.AUTH_TOKEN + userId);
    }

    /**
     * 提取请求头中的令牌（去除前缀）。
     */
    public String resolveToken(String header) {
        String prefix = properties.getJwt().getPrefix();
        if (header == null || header.isBlank()) {
            return null;
        }
        if (prefix != null && !prefix.isBlank() && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }

    public String getHeaderName() {
        return properties.getJwt().getHeader();
    }

    public long getExpireSeconds() {
        return properties.getJwt().getExpireSeconds();
    }
}
