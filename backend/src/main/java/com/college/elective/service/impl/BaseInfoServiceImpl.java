package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.college.elective.common.BusinessException;
import com.college.elective.common.ResultCode;
import com.college.elective.entity.Classroom;
import com.college.elective.entity.Course;
import com.college.elective.entity.Department;
import com.college.elective.entity.Major;
import com.college.elective.entity.Student;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.ClassroomMapper;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.DepartmentMapper;
import com.college.elective.mapper.MajorMapper;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.TeacherMapper;
import com.college.elective.service.BaseInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 基础信息服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseInfoServiceImpl implements BaseInfoService {

    private final DepartmentMapper departmentMapper;
    private final MajorMapper majorMapper;
    private final ClassroomMapper classroomMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;

    // ==================================================================
    //  院系
    // ==================================================================

    @Override
    public List<Department> listDepartments(String keyword) {
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(Department::getDeptName, keyword)
                        .or().like(Department::getDeptCode, keyword))
                .orderByAsc(Department::getSort)
                .orderByAsc(Department::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDepartment(Department department) {
        BusinessException.throwIf(StrUtil.isBlank(department.getDeptCode()), ResultCode.PARAM_ERROR, "院系编码不能为空");
        BusinessException.throwIf(StrUtil.isBlank(department.getDeptName()), ResultCode.PARAM_ERROR, "院系名称不能为空");

        Long count = departmentMapper.selectCount(Wrappers.<Department>lambdaQuery()
                .eq(Department::getDeptCode, department.getDeptCode())
                .ne(department.getId() != null, Department::getId, department.getId()));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS, "院系编码已存在");

        if (department.getSort() == null) {
            department.setSort(0);
        }
        if (department.getId() == null) {
            departmentMapper.insert(department);
        } else {
            departmentMapper.updateById(department);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        BusinessException.throwIf(department == null, ResultCode.DATA_NOT_FOUND);

        Long studentCount = studentMapper.selectCount(Wrappers.<Student>lambdaQuery()
                .eq(Student::getDeptId, id));
        Long teacherCount = teacherMapper.selectCount(Wrappers.<Teacher>lambdaQuery()
                .eq(Teacher::getDeptId, id));
        Long majorCount = majorMapper.selectCount(Wrappers.<Major>lambdaQuery()
                .eq(Major::getDeptId, id));
        if ((studentCount != null && studentCount > 0)
                || (teacherCount != null && teacherCount > 0)
                || (majorCount != null && majorCount > 0)) {
            throw new BusinessException(ResultCode.OPERATION_FORBIDDEN,
                    "该院系下存在专业、学生或教师，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    // ==================================================================
    //  专业
    // ==================================================================

    @Override
    public List<Major> listMajors(Long deptId, String keyword) {
        return majorMapper.selectList(Wrappers.<Major>lambdaQuery()
                .eq(deptId != null, Major::getDeptId, deptId)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(Major::getMajorName, keyword)
                        .or().like(Major::getMajorCode, keyword))
                .orderByAsc(Major::getMajorCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMajor(Major major) {
        BusinessException.throwIf(StrUtil.isBlank(major.getMajorCode()), ResultCode.PARAM_ERROR, "专业编码不能为空");
        BusinessException.throwIf(StrUtil.isBlank(major.getMajorName()), ResultCode.PARAM_ERROR, "专业名称不能为空");
        BusinessException.throwIf(major.getDeptId() == null, ResultCode.PARAM_ERROR, "请选择所属院系");

        Long count = majorMapper.selectCount(Wrappers.<Major>lambdaQuery()
                .eq(Major::getMajorCode, major.getMajorCode())
                .ne(major.getId() != null, Major::getId, major.getId()));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS, "专业编码已存在");

        if (major.getId() == null) {
            majorMapper.insert(major);
        } else {
            majorMapper.updateById(major);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMajor(Long id) {
        Major major = majorMapper.selectById(id);
        BusinessException.throwIf(major == null, ResultCode.DATA_NOT_FOUND);

        Long studentCount = studentMapper.selectCount(Wrappers.<Student>lambdaQuery()
                .eq(Student::getMajorId, id));
        if (studentCount != null && studentCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FORBIDDEN,
                    "该专业下存在 " + studentCount + " 名学生，无法删除");
        }
        majorMapper.deleteById(id);
    }

    // ==================================================================
    //  教室
    // ==================================================================

    @Override
    public List<Classroom> listClassrooms(String keyword) {
        return classroomMapper.selectList(Wrappers.<Classroom>lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(Classroom::getRoomNo, keyword)
                        .or().like(Classroom::getBuilding, keyword))
                .orderByAsc(Classroom::getBuilding)
                .orderByAsc(Classroom::getRoomNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveClassroom(Classroom classroom) {
        BusinessException.throwIf(StrUtil.isBlank(classroom.getRoomNo()), ResultCode.PARAM_ERROR, "教室编号不能为空");
        BusinessException.throwIf(StrUtil.isBlank(classroom.getBuilding()), ResultCode.PARAM_ERROR, "教学楼不能为空");

        Long count = classroomMapper.selectCount(Wrappers.<Classroom>lambdaQuery()
                .eq(Classroom::getRoomNo, classroom.getRoomNo())
                .ne(classroom.getId() != null, Classroom::getId, classroom.getId()));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS, "教室编号已存在");

        if (classroom.getCapacity() == null) {
            classroom.setCapacity(60);
        }
        if (StrUtil.isBlank(classroom.getRoomType())) {
            classroom.setRoomType("NORMAL");
        }
        if (classroom.getStatus() == null) {
            classroom.setStatus(1);
        }
        if (classroom.getId() == null) {
            classroomMapper.insert(classroom);
        } else {
            classroomMapper.updateById(classroom);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClassroom(Long id) {
        Classroom classroom = classroomMapper.selectById(id);
        BusinessException.throwIf(classroom == null, ResultCode.DATA_NOT_FOUND);

        Long courseCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getDeptId, id));
        if (courseCount != null && courseCount > 0) {
            log.warn("教室 {} 已被课程占用 {} 条记录", classroom.getRoomNo(), courseCount);
        }
        classroomMapper.deleteById(id);
    }
}
