package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页统计数据。
 */
@Data
@Schema(description = "统计数据")
public class StatisticsVO {

    @Schema(description = "学生总数")
    private Long studentCount;

    @Schema(description = "教师总数")
    private Long teacherCount;

    @Schema(description = "课程总数")
    private Long courseCount;

    @Schema(description = "选课记录总数")
    private Long selectionCount;

    @Schema(description = "院系总数")
    private Long deptCount;

    @Schema(description = "本学期可选课程数")
    private Long currentSemesterCourseCount;

    @Schema(description = "选课热度 Top 榜")
    private List<CourseRankVO> hotCourses;

    @Schema(description = "各院系选课分布")
    private List<ChartItemVO> deptDistribution;

    @Schema(description = "成绩分布")
    private List<ChartItemVO> scoreDistribution;

    @Data
    @Schema(description = "课程排行项")
    public static class CourseRankVO {
        @Schema(description = "课程ID")
        private Long courseId;
        @Schema(description = "课程名称")
        private String courseName;
        @Schema(description = "教师姓名")
        private String teacherName;
        @Schema(description = "最大容量")
        private Integer maxCapacity;
        @Schema(description = "已选人数")
        private Integer selectedCount;
        @Schema(description = "选课率百分比")
        private Double rate;
    }

    @Data
    @Schema(description = "图表数据项")
    public static class ChartItemVO {
        @Schema(description = "名称")
        private String name;
        @Schema(description = "数值")
        private Long value;
    }
}
