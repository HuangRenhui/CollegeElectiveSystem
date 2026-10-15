package com.college.elective.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件通知配置。
 *
 * <h3>为什么所有 SMTP 参数都收在 {@code elective.mail.*} 下</h3>
 *
 * <p>早期实现把 SMTP 连接信息放在 Spring Boot 标准的 {@code spring.mail.*}
 * 下，依赖 {@code MailSenderAutoConfiguration} 自动装配 {@code JavaMailSender}。
 * 该方案有两个问题：</p>
 *
 * <ol>
 *   <li><b>行为不透明</b>：自动装配的触发条件依赖 {@code spring.mail.host}
 *       等属性的解析时机。实测中即便在 yml 里显式写好了 host，
 *       容器中的 {@code JavaMailSenderImpl} 的 host 仍为空，
 *       而 {@code elective.mail.*} 下的属性却能正常绑定——
 *       两套配置来源、两套解析路径，排查成本很高；</li>
 *   <li><b>配置来源分散</b>：「开关与发件人」在 {@code elective.mail}、
 *       「连接信息」在 {@code spring.mail}，阅读时要来回跳。</li>
 * </ol>
 *
 * <p>现改为：<b>全部参数收归本类</b>，由 {@code MailConfig} 显式创建
 * {@code JavaMailSenderImpl}。配置来源单一、取值时机确定，
 * 且 {@link #isReady()} 退化为简单的「Bean 是否存在」判断。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "elective.mail")
public class MailProperties {

    /**
     * 是否启用邮件通知。
     *
     * <p>默认关闭：邮件是可选增强，且需要有效的 SMTP 配置，
     * 不应让未配置邮件的环境产生告警噪音。</p>
     */
    private boolean enabled = false;

    /** SMTP 服务器主机名。为空则不创建 JavaMailSender，功能自动降级 */
    private String host;

    /**
     * SMTP 端口。465 为 SMTPS（隐式 SSL）、587/25 为 STARTTLS。
     */
    private int port = 465;

    /** SMTP 认证用户名，通常与发件人同域 */
    private String username;

    /** SMTP 认证口令。属机密，仅经环境变量注入 */
    private String password;

    /**
     * 发件人地址。
     *
     * <p>建议写成 {@code 选修课系统 <noreply@xx.edu.cn>} 的形式，
     * 收件人看到的发件人名称更友好。部分 SMTP 服务要求发件人
     * 与认证账号一致，否则会被拒。</p>
     */
    private String from;

    /** 主题前缀，便于收件人一眼识别来信来源 */
    private String subjectPrefix = "【选修课系统】";

    /**
     * 是否使用 SSL（SMTPS，隐式加密，典型端口 465）。
     *
     * <p>{@code true} 对应 {@code mail.smtp.ssl.enable}；
     * 若用 587 端口应改为 {@code false} 并启用 {@link #starttlsEnable}。</p>
     */
    private boolean sslEnable = true;

    /**
     * 是否启用 STARTTLS（显式加密升级，典型端口 587）。
     */
    private boolean starttlsEnable = false;

    /** 建立连接超时（毫秒） */
    private int connectionTimeout = 5000;

    /** 读取超时（毫秒） */
    private int timeout = 5000;

    /** 写超时（毫秒） */
    private int writeTimeout = 5000;

    /**
     * 配置层面是否就绪。
     *
     * <p>注意这只是「配置是否填全」，与「容器中是否有可用的
     * {@code JavaMailSender} Bean」是两件事——后者由
     * {@code MailNoticeService#isEnabled()} 综合判断。</p>
     *
     * @return true 表示已启用、且 host 与发件人均已填写
     */
    public boolean isReady() {
        return enabled
                && host != null && !host.isBlank()
                && from != null && !from.isBlank();
    }
}
