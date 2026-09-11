package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教师 Mapper。
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    /**
     * 分页查询教师（联表用户、院系）。
     */
    IPage<Teacher> selectTeacherPage(IPage<Teacher> page,
                                     @Param("keyword") String keyword,
                                     @Param("deptId") Long deptId,
                                     @Param("title") String title);

    /**
     * 查询教师详情。
     */
    Teacher selectTeacherDetail(@Param("id") Long id);
}
