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
     */
    Semester getCurrentSemester();

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
