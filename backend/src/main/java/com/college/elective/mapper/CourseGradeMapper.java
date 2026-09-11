package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.CourseGrade;
import com.college.elective.vo.StatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成绩 Mapper。
 */
@Mapper
public interface CourseGradeMapper extends BaseMapper<CourseGrade> {

    /**
     * 分页查询成绩（按学生或课程维度）。
     */
    IPage<CourseGrade> selectGradePage(IPage<CourseGrade> page,
                                       @Param("studentId") Long studentId,
                                       @Param("courseId") Long courseId,
                                       @Param("semesterId") Long semesterId,
                                       @Param("status") Integer status);

    /**
     * 查询学生某学期成绩单明细。
     */
    List<CourseGrade> selectStudentGrades(@Param("studentId") Long studentId,
                                          @Param("semesterId") Long semesterId,
                                          @Param("publishedOnly") boolean publishedOnly);

    /**
     * 按分数段统计成绩分布。
     */
    List<StatisticsVO.ChartItemVO> selectScoreDistribution(@Param("courseId") Long courseId,
                                                           @Param("semesterId") Long semesterId);
}
