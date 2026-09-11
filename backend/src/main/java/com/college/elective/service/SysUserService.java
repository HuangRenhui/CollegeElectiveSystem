package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.dto.ChangePasswordDTO;
import com.college.elective.dto.LoginDTO;
import com.college.elective.entity.SysUser;
import com.college.elective.vo.LoginVO;
import com.college.elective.vo.UserInfoVO;

/**
 * 用户与认证服务。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 登录并签发令牌。
     */
    LoginVO login(LoginDTO loginDTO, String clientIp);

    /**
     * 退出登录，注销令牌。
     */
    void logout();

    /**
     * 获取当前登录用户详细信息。
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 修改当前用户密码。
     */
    void changePassword(ChangePasswordDTO dto);

    /**
     * 更新当前用户基础资料。
     */
    void updateProfile(SysUser user);

    /**
     * 重置指定用户密码（管理员）。
     */
    void resetPassword(Long userId, String newPassword);
}
