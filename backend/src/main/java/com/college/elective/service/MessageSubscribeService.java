package com.college.elective.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.college.elective.common.BusinessException;
import com.college.elective.common.ResultCode;
import com.college.elective.config.WxProperties;
import com.college.elective.entity.MessageSubscribe;
import com.college.elective.entity.UserWxBind;
import com.college.elective.mapper.MessageSubscribeMapper;
import com.college.elective.mapper.UserWxBindMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 微信订阅消息服务。
 *
 * <p>难点在于微信规则：用户每次授权只能收到一条消息，用完即失效，
 * 因此维护剩余可发送次数（授权 +1、发送 -1）。</p>
 *
 * <p>续期设计：不在小程序启动时弹窗索权（会被直接拒绝且拉低好感度），
 * 而是在「评价提交成功」「成绩发布查询」等用户明确需要通知的时刻再引导。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSubscribeService {

    /** 场景：评价开放 */
    public static final String SCENE_REVIEW_OPEN = "REVIEW_OPEN";

    /** 场景：成绩发布 */
    public static final String SCENE_GRADE_PUBLISH = "GRADE_PUBLISH";

    /** 微信订阅关系有效期（天） */
    private static final int EXPIRE_DAYS = 7;

    private final MessageSubscribeMapper subscribeMapper;

    private final UserWxBindMapper bindMapper;

    private final WxMiniAppClient wxClient;

    private final WxProperties wxProperties;

    /**
     * 返回已配置模板的场景，供小程序判断哪些订阅入口需要展示。
     *
     * <p>未配置模板 ID 的场景不返回，避免用户点了却收不到。</p>
     */
    public List<Map<String, String>> listAvailableTemplates() {
        String review = wxProperties.getReviewOpenTemplateId();
        String grade = wxProperties.getGradePublishTemplateId();
        List<Map<String, String>> list = new ArrayList<>();
        if (review != null && !review.isBlank()) {
            list.add(Map.of("scene", SCENE_REVIEW_OPEN, "templateId", review));
        }
        if (grade != null && !grade.isBlank()) {
            list.add(Map.of("scene", SCENE_GRADE_PUBLISH, "templateId", grade));
        }
        return list;
    }

    /**
     * 记录一次订阅授权（剩余次数 +1）。
     *
     * <p>采用累加而非覆盖：用户可能连续授权多次，每次都应计入。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(Long userId, String scene) {
        String templateId = wxProperties.templateIdOf(scene);
        BusinessException.throwIf(templateId == null || templateId.isBlank(),
                ResultCode.SUBSCRIBE_FAILED, "该场景未配置订阅模板");

        LocalDateTime expire = LocalDateTime.now().plusDays(EXPIRE_DAYS);
        MessageSubscribe existing = subscribeMapper.selectOne(
                Wrappers.<MessageSubscribe>lambdaQuery()
                        .eq(MessageSubscribe::getUserId, userId)
                        .eq(MessageSubscribe::getScene, scene));

        if (existing == null) {
            MessageSubscribe record = new MessageSubscribe();
            record.setUserId(userId);
            record.setTemplateId(templateId);
            record.setScene(scene);
            record.setRemainCount(1);
            record.setExpireTime(expire);
            subscribeMapper.insert(record);
            return;
        }

        // 用 SQL 表达式递增：updateById 默认忽略 null，且需保证原子性
        subscribeMapper.update(null, Wrappers.<MessageSubscribe>lambdaUpdate()
                .eq(MessageSubscribe::getId, existing.getId())
                .setSql("remain_count = remain_count + 1")
                .set(MessageSubscribe::getExpireTime, expire)
                .set(MessageSubscribe::getTemplateId, templateId));
    }

    /**
     * 查询某场景的剩余订阅次数。
     *
     * @return 剩余次数；无记录或已过期返回 0
     */
    public int getRemainCount(Long userId, String scene) {
        MessageSubscribe record = subscribeMapper.selectOne(
                Wrappers.<MessageSubscribe>lambdaQuery()
                        .eq(MessageSubscribe::getUserId, userId)
                        .eq(MessageSubscribe::getScene, scene));
        if (record == null || record.getExpireTime().isBefore(LocalDateTime.now())) {
            return 0;
        }
        return record.getRemainCount() == null ? 0 : record.getRemainCount();
    }

    /**
     * 向单个用户推送订阅消息。
     *
     * <p><b>为什么必须先扣减再发送</b>：微信侧的次数扣减是隐式的，
     * 若先发送后扣减，一旦进程崩溃就会超发——而微信会直接拒绝超额请求，
     * 导致用户频繁收不到消息。先扣减则最多漏发，不会超发。</p>
     *
     * @param userId 接收用户
     * @param scene  场景
     * @param data   模板字段
     * @return 是否发送成功
     */
    public boolean pushToUser(Long userId, String scene, Map<String, Object> data) {
        UserWxBind bind = bindMapper.selectOne(Wrappers.<UserWxBind>lambdaQuery()
                .eq(UserWxBind::getUserId, userId));
        if (bind == null) {
            return false;
        }
        String templateId = wxProperties.templateIdOf(scene);
        if (templateId == null || templateId.isBlank()) {
            return false;
        }

        // 原子扣减：仅当仍有剩余次数且未过期时才更新成功
        int updated = subscribeMapper.update(null, Wrappers.<MessageSubscribe>lambdaUpdate()
                .eq(MessageSubscribe::getUserId, userId)
                .eq(MessageSubscribe::getScene, scene)
                .gt(MessageSubscribe::getRemainCount, 0)
                .ge(MessageSubscribe::getExpireTime, LocalDateTime.now())
                .setSql("remain_count = remain_count - 1"));
        if (updated == 0) {
            return false;
        }

        // 扣减成功后再发送；失败不回补次数，宁可漏发不可超发
        boolean sent = wxClient.sendSubscribeMessage(bind.getOpenId(), templateId, data);
        if (!sent) {
            log.info("[订阅消息] 发送失败 scene={}, userId={}", scene, userId);
        }
        return sent;
    }

    /**
     * 批量推送给某场景下所有已订阅用户。
     *
     * @param scene       场景
     * @param dataBuilder 按用户ID 构造模板字段
     * @return 成功发送的条数
     */
    public int pushToSubscribers(String scene,
                                 Function<Long, Map<String, Object>> dataBuilder) {
        List<MessageSubscribe> records = subscribeMapper.selectList(
                Wrappers.<MessageSubscribe>lambdaQuery()
                        .eq(MessageSubscribe::getScene, scene)
                        .gt(MessageSubscribe::getRemainCount, 0)
                        .ge(MessageSubscribe::getExpireTime, LocalDateTime.now()));
        int success = 0;
        for (MessageSubscribe record : records) {
            try {
                if (pushToUser(record.getUserId(), scene, dataBuilder.apply(record.getUserId()))) {
                    success++;
                }
            } catch (Exception e) {
                // 单个用户失败不影响其余用户
                log.warn("[订阅消息] 批量推送异常 userId={}", record.getUserId(), e);
            }
        }
        log.info("[订阅消息] 批量推送完成 scene={}, 目标={}, 成功={}",
                scene, records.size(), success);
        return success;
    }
}
