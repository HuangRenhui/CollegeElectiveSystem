package com.college.elective.config;

import com.college.elective.security.JwtAuthenticationFilter;
import com.college.elective.security.RestAccessDeniedHandler;
import com.college.elective.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：无状态 JWT 认证 + 方法级权限控制。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final ElectiveProperties properties;

    /**
     * 无需认证即可访问的路径。
     *
     * <p><b>为什么微信的三个接口必须放行</b>：它们都在「用户还没拿到系统令牌」
     * 的阶段被调用——</p>
     * <ul>
     *   <li>{@code /wx/enabled}：小程序登录页探测是否展示微信入口，此时尚未登录；</li>
     *   <li>{@code /wx/login}：用 {@code wx.login()} 的 code 换身份，本就是登录动作；</li>
     *   <li>{@code /wx/bind}：首次绑定，此时也还没有系统令牌。</li>
     * </ul>
     * <p>三者若被鉴权拦截，微信一键登录链路会在第一步就 401，
     * 功能整体不可用（且接口自身逻辑正确，问题只在白名单）。</p>
     *
     * <p>注意<b>不要</b>写成 {@code /wx/**}：{@code /wx/subscribe} 系列依赖
     * {@code SecurityUtils.getUserId()} 取当前用户，必须保持鉴权。</p>
     */
    private static final String[] WHITE_LIST = {
            "/auth/login",
            "/wx/enabled",
            "/wx/login",
            "/wx/bind",
            "/doc.html",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> registry
                        // 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 公告查询对登录用户开放
                        .requestMatchers(HttpMethod.GET, "/notice/**").authenticated()
                        // 管理员专属
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 教师专属
                        .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        // 学生专属
                        .requestMatchers("/student/**").hasAnyRole("STUDENT", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * 密码编码器：默认 bcrypt，同时兼容数据库中 {noop} 前缀的明文初始化密码。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(properties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
