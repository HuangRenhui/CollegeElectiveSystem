package com.college.elective.security;

import com.college.elective.common.BusinessException;
import com.college.elective.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 安全上下文工具：快速获取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<LoginUser> getLoginUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return Optional.empty();
        }
        return Optional.of(loginUser);
    }

    public static LoginUser getLoginUser() {
        return getLoginUserOptional()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static String getRole() {
        return getLoginUser().getRole();
    }

    /**
     * 获取当前学生ID，非学生身份将抛出异常。
     */
    public static Long requireStudentId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser.getStudentId() == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_ALLOWED, "当前账号未绑定学生信息");
        }
        return loginUser.getStudentId();
    }

    /**
     * 获取当前教师ID，非教师身份将抛出异常。
     */
    public static Long requireTeacherId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser.getTeacherId() == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_ALLOWED, "当前账号未绑定教师信息");
        }
        return loginUser.getTeacherId();
    }
}
