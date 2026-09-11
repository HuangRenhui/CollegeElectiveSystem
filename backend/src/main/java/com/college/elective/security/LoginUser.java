package com.college.elective.security;

import com.college.elective.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 登录用户上下文，存放在 Spring Security 的 SecurityContext 中。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;
    /** 登录账号 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 角色：STUDENT/TEACHER/ADMIN */
    private String role;
    /** 学生ID（角色为 STUDENT 时有效） */
    private Long studentId;
    /** 教师ID（角色为 TEACHER 时有效） */
    private Long teacherId;
    /** 所属院系ID */
    private Long deptId;
    /** 账号是否可用 */
    private Boolean enabled = true;

    public LoginUser(Long userId, String username, String realName, String role) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.enabled = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(Constants.ROLE_PREFIX + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isStudent() {
        return Constants.ROLE_STUDENT.equals(role);
    }

    public boolean isTeacher() {
        return Constants.ROLE_TEACHER.equals(role);
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginUser that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }
}
