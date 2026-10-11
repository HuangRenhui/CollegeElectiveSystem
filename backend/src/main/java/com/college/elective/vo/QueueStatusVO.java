package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 选课排队状态。
 *
 * <p>用于选课高峰期的削峰：学生在正式选课前先取号，由服务端按
 * 「每秒放行 N 名」的节奏叫号，叫到号后才允许进入选课核心流程。</p>
 *
 * <p><b>与超卖防护的关系</b>：排队解决的是「峰值请求穿透到数据库」，
 * 而非超选——超选由 {@code select_course.lua} 的原子预占保证。
 * 两者是不同层次的保护，不可相互替代。</p>
 */
@Data
@Schema(description = "选课排队状态")
public class QueueStatusVO {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "我的排队号，由发号器严格递增生成")
    private Long mySeq;

    @Schema(description = "当前已放行到的排队号；mySeq <= currentSeq 即表示已叫号")
    private Long currentSeq;

    @Schema(description = "我前面还有多少人（已叫号时为 0）")
    private Long aheadCount;

    @Schema(description = "当前排队总人数")
    private Long queuedCount;

    @Schema(description = "预计还需等待的秒数")
    private Long estimatedWaitSeconds;

    @Schema(description = "令牌是否有效；无效时需重新取号")
    private Boolean tokenValid;

    @Schema(description = "令牌剩余有效期（秒）")
    private Long tokenTtlSeconds;

    @Schema(description = "提示文案，可直接展示给用户")
    private String message;
}