package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.ChangePasswordDTO;
import com.college.elective.dto.LoginDTO;
import com.college.elective.entity.Department;
import com.college.elective.entity.Major;
import com.college.elective.entity.Student;
import com.college.elective.entity.SysUser;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.DepartmentMapper;
import com.college.elective.mapper.MajorMapper;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.mapper.TeacherMapper;
import com.college.elective.security.JwtTokenProvider;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.SysUserService;
import com.college.elective.vo.LoginVO;
import com.college.elective.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户与认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /** 连续登录失败锁定阈值 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 锁定时间（分钟） */
    private static final long LOCK_MINUTES = 15L;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final DepartmentMapper departmentMapper;
    private final MajorMapper majorMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO, String clientIp) {
        String username = StrUtil.trim(loginDTO.getUsername());
        String failKey = RedisKeys.LOGIN_FAIL + username;

        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= MAX_LOGIN_FAIL) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED,
                    "登录失败次数过多，账号已临时锁定，请 " + LOCK_MINUTES + " 分钟后再试");
        }

        SysUser user = getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));

        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        if (Constants.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 登录成功：清理失败计数、写入登录扩展信息缓存
        stringRedisTemplate.delete(failKey);
        cacheUserExtension(user);

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 记录登录痕迹
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(clientIp);
        updateById(update);

        log.info("用户登录成功: {} ({}) IP={}", user.getUsername(), user.getRole(), clientIp);

        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpireSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }

    private void recordLoginFail(String failKey, int currentCount) {
        stringRedisTemplate.opsForValue().increment(failKey);
        if (currentCount == 0) {
            stringRedisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 缓存学生的 studentId/teacherId/deptId，供 JWT 过滤器还原登录上下文。
     */
    private void cacheUserExtension(SysUser user) {
        Map<String, String> extension = new HashMap<>(4);
        if (Constants.ROLE_STUDENT.equals(user.getRole())) {
            Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                    .eq(Student::getUserId, user.getId()).last("LIMIT 1"));
            if (student != null) {
                extension.put("studentId", String.valueOf(student.getId()));
                extension.put("deptId", String.valueOf(student.getDeptId()));
            }
        } else if (Constants.ROLE_TEACHER.equals(user.getRole())) {
            Teacher teacher = teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery()
                    .eq(Teacher::getUserId, user.getId()).last("LIMIT 1"));
            if (teacher != null) {
                extension.put("teacherId", String.valueOf(teacher.getId()));
                extension.put("deptId", String.valueOf(teacher.getDeptId()));
            }
        }
        if (!extension.isEmpty()) {
            String key = RedisKeys.AUTH_USER + user.getId();
            stringRedisTemplate.opsForHash().putAll(key, extension);
            stringRedisTemplate.expire(key, tokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void logout() {
        SecurityUtils.getLoginUserOptional().ifPresent(loginUser -> {
            tokenProvider.invalidateToken(loginUser.getUserId());
            stringRedisTemplate.delete(RedisKeys.AUTH_USER + loginUser.getUserId());
            log.info("用户退出登录: {}", loginUser.getUsername());
        });
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = getById(loginUser.getUserId());
        BusinessException.throwIf(user == null, ResultCode.USER_NOT_FOUND);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());

        if (Constants.ROLE_STUDENT.equals(user.getRole())) {
            Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                    .eq(Student::getUserId, user.getId()).last("LIMIT 1"));
            if (student != null) {
                vo.setStuNo(student.getStuNo());
                vo.setClassName(student.getClassName());
                vo.setTotalCredit(student.getTotalCredit());
                fillDeptAndMajor(vo, student.getDeptId(), student.getMajorId());
            }
        } else if (Constants.ROLE_TEACHER.equals(user.getRole())) {
            Teacher teacher = teacherMapper.selectOne(Wrappers.<Teacher>lambdaQuery()
                    .eq(Teacher::getUserId, user.getId()).last("LIMIT 1"));
            if (teacher != null) {
                vo.setTeacherNo(teacher.getTeacherNo());
                vo.setTitle(teacher.getTitle());
                fillDeptAndMajor(vo, teacher.getDeptId(), null);
            }
        }
        return vo;
    }

    private void fillDeptAndMajor(UserInfoVO vo, Long deptId, Long majorId) {
        if (deptId != null) {
            Department department = departmentMapper.selectById(deptId);
            if (department != null) {
                vo.setDeptName(department.getDeptName());
            }
        }
        if (majorId != null) {
            Major major = majorMapper.selectById(majorId);
            if (major != null) {
                vo.setMajorName(major.getMajorName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        Long userId = SecurityUtils.getUserId();
        SysUser user = getById(userId);
        BusinessException.throwIf(user == null, ResultCode.USER_NOT_FOUND);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(update);

        // 密码变更后强制重新登录
        tokenProvider.invalidateToken(userId);
        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(SysUser user) {
        Long userId = SecurityUtils.getUserId();
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(user.getRealName());
        update.setGender(user.getGender());
        update.setPhone(user.getPhone());
        update.setEmail(user.getEmail());
        update.setAvatar(user.getAvatar());
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        BusinessException.throwIf(user == null, ResultCode.USER_NOT_FOUND);
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码长度不能少于 6 位");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        updateById(update);
        tokenProvider.invalidateToken(userId);
        log.info("管理员重置用户 {} 的密码", user.getUsername());
    }
}
