package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生 Mapper。
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生（联表用户、院系、专业）。
     */
    IPage<Student> selectStudentPage(IPage<Student> page,
                                     @Param("keyword") String keyword,
                                     @Param("deptId") Long deptId,
                                     @Param("majorId") Long majorId,
                                     @Param("gradeYear") Integer gradeYear);

    /**
     * 查询学生详情（含用户、院系、专业信息）。
     */
    Student selectStudentDetail(@Param("id") Long id);
}
