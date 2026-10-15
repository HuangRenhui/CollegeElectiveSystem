package com.college.elective.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件发送器配置。
 *
 * <p>显式创建 {@link JavaMailSenderImpl}，而非依赖 Spring Boot 的
 * {@code MailSenderAutoConfiguration} 条件装配。原因见
 * {@link MailProperties} 的类注释。</p>
 *
 * <p><b>不用 {@code @Bean} 返回 null 来表达「不启用」</b>：
 * Spring 会把 null 包装成 {@code NullBean} 注册，注入方
 * （即使经 {@code ObjectProvider}）拿到的语义会变模糊，
 * 顺序问题下甚至可能抛 {@code NoSuchBeanDefinitionException}。
 * 这里改用自定义 {@link Conditional}，条件不满足时<b>根本不注册</b>该 Bean，
 * 语义最干净。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final MailProperties props;

    /**
     * 仅当「已启用 + host 非空」时创建 {@code JavaMailSender}。
     */
    @Bean
    @ConditionalOnProperty(prefix = "elective.mail", name = "enabled", havingValue = "true")
    @Conditional(MailConfig.HostPresentCondition.class)
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort());
        sender.setDefaultEncoding("UTF-8");

        boolean hasAuth = props.getUsername() != null && !props.getUsername().isBlank();
        if (hasAuth) {
            sender.setUsername(props.getUsername());
            sender.setPassword(props.getPassword());
        }

        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        // 有用户名才开启认证，否则部分无需认证的内网 SMTP 会被拒
        p.put("mail.smtp.auth", String.valueOf(hasAuth));
        p.put("mail.smtp.ssl.enable", String.valueOf(props.isSslEnable()));
        p.put("mail.smtp.starttls.enable", String.valueOf(props.isStarttlsEnable()));
        // 三个超时都必须设置，否则 SMTP 无响应时发送线程会被长期占用
        p.put("mail.smtp.connectiontimeout", String.valueOf(props.getConnectionTimeout()));
        p.put("mail.smtp.timeout", String.valueOf(props.getTimeout()));
        p.put("mail.smtp.writetimeout", String.valueOf(props.getWriteTimeout()));

        log.info("[邮件] JavaMailSender 已创建: host={}, port={}, ssl={}, starttls={}, auth={}",
                props.getHost(), props.getPort(), props.isSslEnable(),
                props.isStarttlsEnable(), hasAuth);
        return sender;
    }

    /**
     * host 非空条件。
     *
     * <p>开关打开但 host 未填时，不注册 Bean，并给出明确提示——
     * 若此时注册一个 host 为空的 sender，直到真正发送才报错，排查成本很高。</p>
     */
    static class HostPresentCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String host = context.getEnvironment().getProperty("elective.mail.host");
            boolean ok = host != null && !host.isBlank();
            if (!ok) {
                log.warn("[邮件] elective.mail.enabled=true 但 elective.mail.host 未配置，"
                        + "不创建 JavaMailSender，邮件通知保持关闭");
            }
            return ok;
        }
    }
}
