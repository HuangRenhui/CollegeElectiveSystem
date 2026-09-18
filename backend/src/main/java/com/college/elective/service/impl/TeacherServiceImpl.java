package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.TeacherDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.SysUser;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.mapper.TeacherMapper;
import com.college.elective.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 教师管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<Teacher> pageTeachers(Long pageNum, Long pageSize, String keyword,
                                            Long deptId, String title) {
        IPage<Teacher> page = baseMapper.selectTeacherPage(
                new Page<>(pageNum, pageSize), keyword, deptId, title);
        return PageResult.of(page);
    }

    @Override
    public Teacher getTeacherDetail(Long id) {
        Teacher teacher = baseMapper.selectTeacherDetail(id);
        BusinessException.throwIf(teacher == null, ResultCode.TEACHER_NOT_FOUND);
        return teacher;
    }

    @Override
    public List<Teacher> listOptions(Long deptId) {
        return baseMapper.selectTeacherPage(new Page<>(1, 500), null, deptId, null).getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTeacher(TeacherDTO dto) {
        validateTeacherNoUnique(dto.getTeacherNo(), null);
        validateUsernameUnique(dto.getTeacherNo(), null);

        SysUser user = new SysUser();
        user.setUsername(dto.getTeacherNo());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(dto.getRealName());
        user.setRole(Constants.ROLE_TEACHER);
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(user);

        Teacher teacher = new Teacher();
        teacher.setUserId(user.getId());
        teacher.setTeacherNo(dto.getTeacherNo());
        teacher.setDeptId(dto.getDeptId());
        teacher.setTitle(dto.getTitle());
        teacher.setResearchArea(dto.getResearchArea());
        save(teacher);

        log.info("新增教师成功: {} ({})，初始密码 {}", dto.getRealName(), dto.getTeacherNo(), DEFAULT_PASSWORD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeacher(TeacherDTO dto) {
        BusinessException.throwIf(dto.getId() == null, ResultCode.PARAM_ERROR, "教师ID不能为空");
        Teacher exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ResultCode.TEACHER_NOT_FOUND);
        validateTeacherNoUnique(dto.getTeacherNo(), dto.getId());
        validateUsernameUnique(dto.getTeacherNo(), exist.getUserId());

        Teacher teacher = new Teacher();
        teacher.setId(dto.getId());
        teacher.setTeacherNo(dto.getTeacherNo());
        teacher.setDeptId(dto.getDeptId());
        teacher.setTitle(dto.getTitle());
        teacher.setResearchArea(dto.getResearchArea());
        updateById(teacher);

        SysUser user = new SysUser();
        user.setId(exist.getUserId());
        user.setUsername(dto.getTeacherNo());
        user.setRealName(dto.getRealName());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userMapper.updateById(user);
    }

    /**
     * 校验工号在 teacher 表中是否唯一。
     *
     * @param teacherNo 待校验的工号
     * @param excludeId 需要排除的教师ID，编辑时传入自身ID以避免误判，新增时传 {@code null}
     */
    private void validateTeacherNoUnique(String teacherNo, Long excludeId) {
        Long count = baseMapper.selectCount(Wrappers.<Teacher>lambdaQuery()
                .eq(Teacher::getTeacherNo, teacherNo)
                .ne(excludeId != null, Teacher::getId, excludeId));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS, "工号已存在");
    }

    /**
     * 校验工号作为登录账号是否唯一。
     *
     * <p>教师的工号会同时写入 {@code teacher.teacher_no} 与 {@code sys_user.username}，
     * 而 {@code sys_user} 为师生共用的用户表。仅校验 {@code teacher} 表无法覆盖
     * 「该工号已被学生学号或管理员账号占用」的情况，届时 {@code updateById} 会触发
     * 数据库唯一索引异常，返回给前端的报错难以理解。故此处单独校验一次，
     * 以便给出明确的业务提示。</p>
     *
     * @param username      待校验的登录账号（即工号）
     * @param excludeUserId 需要排除的用户ID，编辑时传入该教师原有的用户ID，新增时传 {@code null}
     */
    private void validateUsernameUnique(String username, Long excludeUserId) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeUserId != null, SysUser::getId, excludeUserId));
        BusinessException.throwIf(count != null && count > 0, ResultCode.USERNAME_EXISTS,
                "登录账号 " + username + " 已存在");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeacher(Long id) {
        Teacher teacher = getById(id);
        BusinessException.throwIf(teacher == null, ResultCode.TEACHER_NOT_FOUND);

        Long courseCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getTeacherId, id));
        if (courseCount != null && courseCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FORBIDDEN,
                    "该教师仍有 " + courseCount + " 门授课课程，无法删除");
        }

        removeById(id);
        if (teacher.getUserId() != null) {
            SysUser user = new SysUser();
            user.setId(teacher.getUserId());
            user.setStatus(Constants.STATUS_DISABLED);
            userMapper.updateById(user);
        }
        log.info("删除教师成功: {}", teacher.getTeacherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Teacher teacher = getById(id);
        BusinessException.throwIf(teacher == null, ResultCode.TEACHER_NOT_FOUND);

        SysUser user = new SysUser();
        user.setId(teacher.getUserId());
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public Teacher getByUserId(Long userId) {
        return getOne(Wrappers.<Teacher>lambdaQuery()
                .eq(Teacher::getUserId, userId)
                .last("LIMIT 1"));
    }
}
