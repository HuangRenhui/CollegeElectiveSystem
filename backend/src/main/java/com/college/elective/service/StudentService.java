package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.StudentDTO;
import com.college.elective.entity.Student;

/**
 * 学生管理服务。
 */
public interface StudentService extends IService<Student> {

    /**
     * 分页查询学生。
     */
    PageResult<Student> pageStudents(Long pageNum, Long pageSize, String keyword,
                                     Long deptId, Long majorId, Integer gradeYear);

    /**
     * 查询学生详情。
     */
    Student getStudentDetail(Long id);

    /**
     * 新增学生（同时创建登录账号，初始密码为学号）。
     */
    void createStudent(StudentDTO dto);

    /**
     * 修改学生信息。
     */
    void updateStudent(StudentDTO dto);

    /**
     * 删除学生（同时禁用账号）。
     */
    void deleteStudent(Long id);

    /**
     * 启用/禁用学生账号。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据用户ID查询学生。
     */
    Student getByUserId(Long userId);
}
