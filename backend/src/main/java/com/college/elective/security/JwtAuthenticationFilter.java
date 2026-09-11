package com.college.elective.security;

import com.college.elective.common.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：解析请求头令牌并注入 SecurityContext。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = tokenProvider.resolveToken(request.getHeader(tokenProvider.getHeaderName()));

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseToken(token);
            if (claims != null) {
                Long userId = Long.valueOf(claims.getSubject());
                // 校验令牌白名单，支持服务端主动注销
                if (tokenProvider.isTokenValid(userId, token)) {
                    LoginUser loginUser = buildLoginUser(claims);

                    // 滑动过期：剩余有效期不足时自动续期
                    if (tokenProvider.shouldRefresh(claims)) {
                        tokenProvider.refreshToken(userId, token);
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 JWT 声明还原登录用户，学生/教师ID 由 Redis 中的登录缓存补充。
     */
    private LoginUser buildLoginUser(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get("role", String.class);
        LoginUser loginUser = new LoginUser(
                userId,
                claims.get("username", String.class),
                claims.get("realName", String.class),
                role);

        if (!Constants.ROLE_ADMIN.equals(role)) {
            try {
                var operations = stringRedisTemplate.opsForHash();
                String cacheKey = com.college.elective.common.RedisKeys.AUTH_USER + userId;
                String studentId = (String) operations.get(cacheKey, "studentId");
                String teacherId = (String) operations.get(cacheKey, "teacherId");
                String deptId = (String) operations.get(cacheKey, "deptId");
                if (StringUtils.hasText(studentId)) {
                    loginUser.setStudentId(Long.valueOf(studentId));
                }
                if (StringUtils.hasText(teacherId)) {
                    loginUser.setTeacherId(Long.valueOf(teacherId));
                }
                if (StringUtils.hasText(deptId)) {
                    loginUser.setDeptId(Long.valueOf(deptId));
                }
            } catch (Exception e) {
                log.warn("读取登录用户扩展信息失败, userId={}", userId, e);
            }
        }
        return loginUser;
    }
}
