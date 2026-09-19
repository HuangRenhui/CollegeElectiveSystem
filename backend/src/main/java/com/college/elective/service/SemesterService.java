package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.entity.Semester;

import java.util.List;

/**
 * 学期服务。
 */
public interface SemesterService extends IService<Semester> {

    /**
     * 分页查询学期。
     */
    PageResult<Semester> pageSemesters(Long pageNum, Long pageSize, String keyword);

    /**
     * 查询全部学期（下拉选项用）。
     */
    List<Semester> listAll();

    /**
     * 获取当前学期。
     *
     * @return 当前学期；未设置当前学期时返回 {@code null}
     */
    Semester getCurrentSemester();

    /**
     * 获取当前学期ID。
     *
     * <p>课程查询、课表、统计与选课窗口校验均以当前学期为基准，
     * 统一从此处获取，避免各处重复实现。</p>
     *
     * @return 当前学期ID；未设置当前学期时返回 {@code null}
     */
    Long getCurrentSemesterId();

    /**
     * 新增/修改学期。
     */
    void saveSemester(Semester semester);

    /**
     * 设为当前学期（互斥）。
     */
    void setCurrent(Long id);

    /**
     * 删除学期（存在课程时禁止删除）。
     */
    void deleteSemester(Long id);
}
