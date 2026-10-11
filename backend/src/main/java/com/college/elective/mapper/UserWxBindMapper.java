package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.college.elective.entity.UserWxBind;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微信身份绑定 Mapper。
 *
 * <p>绑定关系以 {@code (wx_appid, open_id)} 唯一，由数据库唯一索引兜底，
 * 避免并发下同一微信身份被绑到多个系统账号。</p>
 */
@Mapper
public interface UserWxBindMapper extends BaseMapper<UserWxBind> {
}
