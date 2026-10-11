package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.college.elective.entity.MessageSubscribe;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息订阅关系 Mapper。
 *
 * <p>订阅关系以 {@code (user_id, scene)} 唯一，剩余次数与过期时间
 * 由 {@code MessageSubscribeService} 维护。</p>
 */
@Mapper
public interface MessageSubscribeMapper extends BaseMapper<MessageSubscribe> {
}
