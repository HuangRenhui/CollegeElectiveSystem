package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.StudentDTO;
import com.college.elective.entity.Student;
import com.college.elective.entity.SysUser;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    /** 新学生默认初始密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<Student> pageStudents(Long pageNum, Long pageSize, String keyword,
                                            Long deptId, Long majorId, Integer gradeYear) {
        IPage<Student> page = baseMapper.selectStudentPage(
                new Page<>(pageNum, pageSize), keyword, deptId, majorId, gradeYear);
        return PageResult.of(page);
    }

    @Override
    public Student getStudentDetail(Long id) {
        Student student = baseMapper.selectStudentDetail(id);
        BusinessException.throwIf(student == null, ResultCode.STUDENT_NOT_FOUND);
        return student;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStudent(StudentDTO dto) {
        validateStuNoUnique(dto.getStuNo(), null);

        Long userCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getStuNo()));
        BusinessException.throwIf(userCount != null && userCount > 0, ResultCode.USERNAME_EXISTS,
                "登录账号 " + dto.getStuNo() + " 已存在");

        SysUser user = new SysUser();
        user.setUsername(dto.getStuNo());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(dto.getRealName());
        user.setRole(Constants.ROLE_STUDENT);
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(user);

        Student student = new Student();
        student.setUserId(user.getId());
        student.setStuNo(dto.getStuNo());
        student.setDeptId(dto.getDeptId());
        student.setMajorId(dto.getMajorId());
        student.setClassName(dto.getClassName());
        student.setGradeYear(dto.getGradeYear() == null ? 1 : dto.getGradeYear());
        student.setEnrollYear(dto.getEnrollYear() == null
                ? LocalDateTime.now().getYear() : dto.getEnrollYear());
        student.setTotalCredit(BigDecimal.ZERO);
        save(student);

        log.info("新增学生成功: {} ({})，初始密码 {}",
                dto.getRealName(), dto.getStuNo(), DEFAULT_PASSWORD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(StudentDTO dto) {
        BusinessException.throwIf(dto.getId() == null, ResultCode.PARAM_ERROR, "学生ID不能为空");
        Student exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ResultCode.STUDENT_NOT_FOUND);
        validateStuNoUnique(dto.getStuNo(), dto.getId());

        Student student = new Student();
        student.setId(dto.getId());
        student.setStuNo(dto.getStuNo());
        student.setDeptId(dto.getDeptId());
        student.setMajorId(dto.getMajorId());
        student.setClassName(dto.getClassName());
        student.setGradeYear(dto.getGradeYear());
        student.setEnrollYear(dto.getEnrollYear());
        updateById(student);

        SysUser user = new SysUser();
        user.setId(exist.getUserId());
        user.setUsername(dto.getStuNo());
        user.setRealName(dto.getRealName());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userMapper.updateById(user);
    }

    private void validateStuNoUnique(String stuNo, Long excludeId) {
        Long count = baseMapper.selectCount(Wrappers.<Student>lambdaQuery()
                .eq(Student::getStuNo, stuNo)
                .ne(excludeId != null, Student::getId, excludeId));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS, "学号已存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(Long id) {
        Student student = getById(id);
        BusinessException.throwIf(student == null, ResultCode.STUDENT_NOT_FOUND);

        Long selectionCount = baseMapper.selectCount(Wrappers.<Student>lambdaQuery()
                .eq(Student::getId, id));
        removeById(id);

        // 逻辑删除用户，禁止其继续登录
        if (student.getUserId() != null) {
            SysUser user = new SysUser();
            user.setId(student.getUserId());
            user.setStatus(Constants.STATUS_DISABLED);
            userMapper.updateById(user);
        }
        log.info("删除学生成功: {} (关联选课记录 {} 条)", student.getStuNo(), selectionCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Student student = getById(id);
        BusinessException.throwIf(student == null, ResultCode.STUDENT_NOT_FOUND);

        SysUser user = new SysUser();
        user.setId(student.getUserId());
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("学生 {} 账号状态变更为 {}", student.getStuNo(), status);
    }

    @Override
    public Student getByUserId(Long userId) {
        return getOne(Wrappers.<Student>lambdaQuery()
                .eq(Student::getUserId, userId)
                .last("LIMIT 1"));
    }
}
