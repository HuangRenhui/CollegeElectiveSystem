package com.college.elective.service;

import com.college.elective.entity.Classroom;
import com.college.elective.entity.Department;
import com.college.elective.entity.Major;

import java.util.List;

/**
 * 基础信息（院系、专业、教室）服务。
 */
public interface BaseInfoService {

    // ---------------- 院系 ----------------

    List<Department> listDepartments(String keyword);

    void saveDepartment(Department department);

    void deleteDepartment(Long id);

    // ---------------- 专业 ----------------

    List<Major> listMajors(Long deptId, String keyword);

    void saveMajor(Major major);

    void deleteMajor(Long id);

    // ---------------- 教室 ----------------

    List<Classroom> listClassrooms(String keyword);

    void saveClassroom(Classroom classroom);

    void deleteClassroom(Long id);
}
