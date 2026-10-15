package com.college.elective.service;

import com.college.elective.common.BusinessException;
import com.college.elective.common.ResultCode;
import com.college.elective.config.MailProperties;
import com.college.elective.entity.SysUser;
import com.college.elective.mapper.SysUserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 邮件通知服务。
 *
 * <p>与站内信、微信订阅消息构成三通道通知：</p>
 * <ul>
 *   <li><b>站内信</b>：登录后可见，最可靠，不依赖外部服务；</li>
 *   <li><b>邮件</b>：本类，无需登录即可触达，适合「成绩已发布」这类
 *       用户可能长时间不登录的场景；</li>
 *   <li><b>微信订阅消息</b>：触达最快，但受「一次授权只能发一条」限制。</li>
 * </ul>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>全程异步</b>：SMTP 握手有秒级延迟，绝不能阻塞选课/成绩发布请求；</li>
 *   <li><b>逐人发送，不用群发抄送</b>：否则所有收件人邮箱互相可见，
 *       属于隐私泄露。代价是 N 次 SMTP 交互，已由异步线程池吸收；</li>
 *   <li><b>失败只记录不抛出</b>：邮件是尽力送达的提醒，
 *       此时主流程已提交（选课成功/成绩已发布），不应因邮件失败回滚。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailNoticeService {

    private final MailProperties mailProperties;
    private final SysUserMapper sysUserMapper;

    /**
     * 用 {@link ObjectProvider} 而非直接注入：未配置 {@code spring.mail.host} 时
     * Spring 不会创建 {@code JavaMailSender} 这个 Bean，直接注入会导致启动失败。
     */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * 启动时打印邮件通知的就绪状态。
     *
     * <p>用途：邮件是「配置了才生效」的可选能力，若只在发送失败时才发现
     * 未配置，排查成本很高。启动时明确输出一次，运维一眼就能确认
     * 邮件到底有没有生效、缺哪一项。</p>
     *
     * <p>日志级别用 INFO 而非 DEBUG：这是运维需要长期可见的关键配置状态。</p>
     */
    @PostConstruct
    void logReadiness() {
        boolean ready = isEnabled();
        log.info("[邮件] 初始化状态: enabled={}, host={}, from={}, 可发送={}",
                mailProperties.isEnabled(),
                mailProperties.getHost() == null || mailProperties.getHost().isBlank()
                        ? "(未配置)" : mailProperties.getHost(),
                mailProperties.getFrom() == null || mailProperties.getFrom().isBlank()
                        ? "(未配置)" : mailProperties.getFrom(),
                ready);
        if (!ready) {
            log.info("[邮件] 未就绪：选课成功与成绩发布只发站内信，不发送邮件。"
                    + "如需启用请配置 ELECTIVE_MAIL_ENABLED / ELECTIVE_MAIL_HOST / "
                    + "ELECTIVE_MAIL_FROM 等环境变量");
        }
    }

    /**
     * 邮件通知是否可用。
     *
     * <p>改为显式装配 {@code JavaMailSender} 后，判断大幅简化：
     * {@code MailConfig} 只在「已启用 + host 非空」时才注册该 Bean，
     * 因此这里只需检查 <b>Bean 是否存在</b>。</p>
     *
     * <p>此前需要额外判断 {@code JavaMailSenderImpl#getHost()} 是否为空，
     * 是因为自动装配会创建一个 host 为空的 Bean，导致「Bean 存在但不可用」——
     * 那种模糊状态现已从源头消除。</p>
     *
     * @return true 表示可以发送
     */
    public boolean isEnabled() {
        return mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 给单个用户发邮件（异步）。
     *
     * @param userId  接收人 {@code sys_user.id}
     * @param subject 主题（会自动加统一前缀）
     * @param content 正文
     */
    @Async("mailExecutor")
    public void sendToUser(Long userId, String subject, String content) {
        try {
            if (!isEnabled()) {
                log.debug("[邮件] 未启用，跳过 userId={}", userId);
                return;
            }
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                log.warn("[邮件] 跳过：用户不存在 userId={}", userId);
                return;
            }
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                log.debug("[邮件] 跳过：用户未填邮箱 userId={}", userId);
                return;
            }
            doSend(user.getEmail(), subject, content);
        } catch (Exception e) {
            // 邮件失败不影响主流程，仅记录（调用方本就已提交）
            log.warn("[邮件] 发送失败 userId={}, subject={}", userId, subject, e);
        }
    }

    /**
     * 给多个用户发邮件（异步，逐个发送）。
     *
     * @param userIds 接收人 {@code sys_user.id} 集合
     * @param subject 主题
     * @param content 正文
     */
    @Async("mailExecutor")
    public void sendBatch(Collection<Long> userIds, String subject, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        if (!isEnabled()) {
            log.debug("[邮件] 未启用，跳过 {} 人", userIds.size());
            return;
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        int ok = 0;
        int skip = 0;
        int fail = 0;
        for (SysUser user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                skip++;
                continue;
            }
            try {
                doSend(user.getEmail(), subject, content);
                ok++;
            } catch (Exception e) {
                fail++;
                log.warn("[邮件] 发送失败 to={}", maskEmail(user.getEmail()), e);
            }
        }
        log.info("[邮件] 批量发送完成 subject={}, 成功={}, 跳过(无邮箱)={}, 失败={}",
                subject, ok, skip, fail);
    }

    /** 实际发送。收件人地址在日志中脱敏，避免邮箱泄露到日志文件。 */
    private void doSend(String to, String subject, String content) throws Exception {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        BusinessException.throwIf(sender == null, ResultCode.FAIL, "邮件服务不可用");

        MimeMessage message = sender.createMimeMessage();
        // 第二个参数 true 表示 multipart，第三参 charset 用于正确编码中文主题
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.getFrom());
        helper.setTo(to);
        helper.setSubject(mailProperties.getSubjectPrefix() + subject);
        helper.setText(content, true); // true = 按 HTML 解析
        sender.send(message);

        log.info("[邮件] 已发送 to={}, subject={}", maskEmail(to), subject);
    }

    /**
     * 邮箱脱敏：{@code zhangsan@stu.edu.cn} → {@code zh***@stu.edu.cn}。
     */
    private String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 2) + "***" + domain;
    }
}
