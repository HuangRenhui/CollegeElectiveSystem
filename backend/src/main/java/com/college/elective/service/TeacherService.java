package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.TeacherDTO;
import com.college.elective.entity.Teacher;

import java.util.List;

/**
 * 教师管理服务。
 */
public interface TeacherService extends IService<Teacher> {

    /**
     * 分页查询教师。
     */
    PageResult<Teacher> pageTeachers(Long pageNum, Long pageSize, String keyword, Long deptId, String title);

    /**
     * 查询教师详情。
     */
    Teacher getTeacherDetail(Long id);

    /**
     * 下拉选项：全部教师简要信息。
     */
    List<Teacher> listOptions(Long deptId);

    /**
     * 新增教师（同时创建登录账号）。
     */
    void createTeacher(TeacherDTO dto);

    /**
     * 修改教师信息。
     */
    void updateTeacher(TeacherDTO dto);

    /**
     * 删除教师（存在授课课程时禁止删除）。
     */
    void deleteTeacher(Long id);

    /**
     * 启用/禁用教师账号。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据用户ID查询教师。
     */
    Teacher getByUserId(Long userId);
}
