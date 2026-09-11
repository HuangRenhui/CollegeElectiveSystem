package com.college.elective.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI electiveOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("高校选修课管理系统 API")
                        .description("""
                                基于 Spring Boot 3 + Vue 3 + Redis 的高校选修课管理系统。
                                
                                角色说明：
                                - 学生：选课/退课、查看课表与成绩
                                - 教师：课程管理、成绩录入
                                - 管理员：院系专业、学期、课程、公告、日志等教务管理
                                
                                认证方式：先调用 /auth/login 获取 token，再在请求头携带 `Authorization: Bearer {token}`。
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("CollegeElectiveSystem").email("admin@college.edu.cn"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("请输入登录接口返回的 token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
