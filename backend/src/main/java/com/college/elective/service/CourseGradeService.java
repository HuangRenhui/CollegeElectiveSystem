package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.GradeInputDTO;
import com.college.elective.entity.CourseGrade;
import com.college.elective.vo.GradeReportVO;

import java.util.List;

/**
 * 成绩服务。
 *
 * <p>本接口定义成绩管理的能力契约，实现类尚未实现，需由开发者补充编码。
 * 请参考项目文档 {@code docs/待实现功能.md} 中的「成绩模块」章节，
 * 关键业务规则如下：</p>
 *
 * <ul>
 *   <li><b>成绩构成</b>：总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%；
 *       单边成绩缺失时按已有成绩折算。</li>
 *   <li><b>绩点换算</b>：4.0 制分段映射（90+ 为 4.0，60 以下为 0，区间内分档折算）。</li>
 *   <li><b>状态流转</b>：草稿(0) → 已发布(1) → 已归档(2)；已发布/已归档的成绩不可直接修改，
 *       需先撤回发布。</li>
 *   <li><b>发布约束</b>：存在未录入总评成绩的学生时禁止发布。</li>
 *   <li><b>数据联动</b>：发布后将 {@code course_selection.status} 置为 2（已修完），
 *       并回写成绩到 {@code course_selection.score} 便于学生端快速展示。</li>
 *   <li><b>权限校验</b>：教师仅能操作自己授课课程的成绩（管理员除外）。</li>
 * </ul>
 */
public interface CourseGradeService extends IService<CourseGrade> {

    /**
     * 教师批量录入成绩（保存为草稿）。
     *
     * <p>逐条校验成绩取值范围（0-100）；已发布或已归档的记录需拒绝修改；
     * 同时同步写入 {@code course_selection.score}。</p>
     *
     * @param dto 成绩录入参数
     */
    void inputGrades(GradeInputDTO dto);

    /**
     * 发布课程成绩。
     *
     * <p>发布前需校验是否仍有学生未录入总评成绩；发布后同步更新选课记录状态。</p>
     *
     * @param courseId 课程ID
     */
    void publishGrades(Long courseId);

    /**
     * 撤回成绩发布（改回草稿状态）。
     *
     * @param courseId 课程ID
     */
    void revokeGrades(Long courseId);

    /**
     * 分页查询成绩。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param studentId  学生ID（可选）
     * @param courseId   课程ID（可选）
     * @param semesterId 学期ID（可选）
     * @param status     成绩状态（可选）：0-草稿 1-已发布 2-已归档
     * @return 分页成绩记录
     */
    PageResult<CourseGrade> pageGrades(Long pageNum, Long pageSize, Long studentId,
                                       Long courseId, Long semesterId, Integer status);

    /**
     * 查询学生成绩单（含学分与绩点统计）。
     *
     * <p>统计项包括：总学分、已获学分、平均成绩、平均绩点（按学分加权）。</p>
     *
     * @param semesterId 学期ID（可选，为空则统计全部已发布成绩）
     * @return 成绩单
     */
    GradeReportVO getStudentGradeReport(Long semesterId);

    /**
     * 查询某门课程的待录入名单（选课学生 + 已有成绩）。
     *
     * @param courseId 课程ID
     * @return 成绩录入单明细
     */
    List<CourseGrade> listCourseGradeSheet(Long courseId);
}
