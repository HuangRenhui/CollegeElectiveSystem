package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.college.elective.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 公告 Mapper。
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 增加浏览量。
     */
    int increaseViewCount(@Param("id") Long id);
}
