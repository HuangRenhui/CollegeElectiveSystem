package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生成绩单。
 */
@Data
@Schema(description = "学生成绩单")
public class GradeReportVO {

    @Schema(description = "学期名称")
    private String semesterName;

    @Schema(description = "课程成绩明细")
    private List<CourseGrade> grades;

    @Schema(description = "学期总学分")
    private BigDecimal totalCredit;

    @Schema(description = "学期已获学分")
    private BigDecimal earnedCredit;

    @Schema(description = "平均成绩")
    private BigDecimal averageScore;

    @Schema(description = "平均绩点")
    private BigDecimal averageGradePoint;

    @Data
    @Schema(description = "成绩单条目")
    public static class CourseGrade {
        @Schema(description = "课程编号")
        private String courseCode;
        @Schema(description = "课程名称")
        private String courseName;
        @Schema(description = "课程类型")
        private String courseType;
        @Schema(description = "学分")
        private BigDecimal credit;
        @Schema(description = "平时成绩")
        private BigDecimal usualScore;
        @Schema(description = "期末成绩")
        private BigDecimal examScore;
        @Schema(description = "总评成绩")
        private BigDecimal totalScore;
        @Schema(description = "绩点")
        private BigDecimal gradePoint;
        @Schema(description = "是否及格")
        private Boolean pass;
    }
}
