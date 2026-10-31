package com.college.elective.security;

import com.college.elective.common.Constants;
import com.college.elective.common.RedisKeys;
import com.college.elective.entity.Student;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.TeacherMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = tokenProvider.resolveToken(request.getHeader(tokenProvider.getHeaderName()));

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseToken(token);
            Long userId = claims == null ? null : parseUserId(claims);
            // 校验令牌白名单，支持服务端主动注销
            if (userId != null && tokenProvider.isTokenValid(userId, token)) {
                if (tokenProvider.isSessionExpired(claims)) {
                    // 超过绝对有效期：即使白名单仍在也不放行，并顺手清理，
                    // 避免「已经不该存在的会话」继续占用 Redis 直到过期
                    tokenProvider.invalidateToken(userId);
                    log.info("[认证] 会话已超过最大有效期，要求重新登录 userId={}", userId);
                } else {
                    // 滑动过期：剩余有效期不足时自动续期（受绝对有效期约束）
                    if (tokenProvider.shouldRefresh(userId)) {
                        tokenProvider.refreshToken(userId, token);
                    }

                    LoginUser loginUser = buildLoginUser(claims, userId);
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
     * 从令牌声明中解析用户ID，非法值返回 {@code null}。
     *
     * <p>签名校验通过后 subject 理论上一定是数字，但令牌可能是历史版本或人工构造，
     * 直接 {@code Long.valueOf} 抛出的 {@link NumberFormatException} 会穿透过滤器，
     * 变成 500 而不是干净的 401。</p>
     */
    private Long parseUserId(Claims claims) {
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            log.debug("[认证] 令牌 subject 不是合法用户ID: {}", subject);
            return null;
        }
    }

    /**
     * 从 JWT 声明还原登录用户，学生/教师ID 由 Redis 中的登录缓存补充。
     */
    private LoginUser buildLoginUser(Claims claims, Long userId) {
        String role = claims.get("role", String.class);
        LoginUser loginUser = new LoginUser(
                userId,
                claims.get("username", String.class),
                claims.get("realName", String.class),
                role);

        if (!Constants.ROLE_ADMIN.equals(role)) {
            try {
                var operations = stringRedisTemplate.opsForHash();
                String cacheKey = RedisKeys.AUTH_USER + userId;
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
                fillMissingBinding(loginUser, role, userId, cacheKey);
            } catch (Exception e) {
                log.warn("读取登录用户扩展信息失败, userId={}", userId, e);
            }
        }
        return loginUser;
    }

    /**
     * Redis 扩展缓存缺失时回源数据库，避免合法学生被误判为未绑定。
     */
    private void fillMissingBinding(LoginUser loginUser, String role, Long userId, String cacheKey) {
        boolean missingStudent = Constants.ROLE_STUDENT.equals(role) && loginUser.getStudentId() == null;
        boolean missingTeacher = Constants.ROLE_TEACHER.equals(role) && loginUser.getTeacherId() == null;
        if (!missingStudent && !missingTeacher) {
            return;
        }
        if (missingStudent) {
            Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                .eq(Student::getUserId, userId)
                .last("LIMIT 1"));
            if (student != null) {
                loginUser.setStudentId(student.getId());
                loginUser.setDeptId(student.getDeptId());
                cacheBinding(cacheKey, "studentId", student.getId(), "deptId", student.getDeptId());
            }
        }
        if (missingTeacher) {
            Teacher teacher = teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery()
                .eq(Teacher::getUserId, userId)
                .last("LIMIT 1"));
            if (teacher != null) {
                loginUser.setTeacherId(teacher.getId());
                loginUser.setDeptId(teacher.getDeptId());
                cacheBinding(cacheKey, "teacherId", teacher.getId(), "deptId", teacher.getDeptId());
            }
        }
    }

    private void cacheBinding(String cacheKey, String idField, Long id, String deptField, Long deptId) {
        if (id != null) {
            stringRedisTemplate.opsForHash().put(cacheKey, idField, String.valueOf(id));
        }
        if (deptId != null) {
            stringRedisTemplate.opsForHash().put(cacheKey, deptField, String.valueOf(deptId));
        }
        stringRedisTemplate.expire(cacheKey, tokenProvider.getExpireSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    }
}
