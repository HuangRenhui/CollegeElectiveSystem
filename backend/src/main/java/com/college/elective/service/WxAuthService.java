package com.college.elective.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.college.elective.common.BusinessException;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.ResultCode;
import com.college.elective.config.WxProperties;
import com.college.elective.dto.WxBindDTO;
import com.college.elective.dto.WxLoginDTO;
import com.college.elective.entity.SysUser;
import com.college.elective.entity.UserWxBind;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.mapper.UserWxBindMapper;
import com.college.elective.security.JwtTokenProvider;
import com.college.elective.vo.WxBindResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 微信登录服务。
 *
 * <p><b>安全底线</b>：未绑定过系统账号的微信身份，绝不能直接签发令牌，
 * 否则任意微信用户都能登入系统。因此首次使用必须输入学号与密码完成绑定，
 * 且绑定时校验密码，防止他人冒用他人学号。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxAuthService {

    private static final String PENDING_BIND_KEY = "wx:pending:openid:";
    private static final long PENDING_BIND_MINUTES = 10L;

    private final WxProperties wxProperties;
    private final WxMiniAppClient wxClient;
    private final UserWxBindMapper bindMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /** 微信登录能力是否可用，未配置 AppID/Secret 时为 false */
    public boolean isEnabled() {
        return wxProperties.isEnabled();
    }

    /** 微信一键登录：已绑定直接签发令牌，未绑定返回待绑定标识 */
    public WxBindResultVO wxLogin(WxLoginDTO dto) {
        BusinessException.throwIf(!wxProperties.isEnabled(), ResultCode.WX_SERVICE_UNAVAILABLE);

        // code 换 openid。session_key 仅服务端持有，绝不外传
        WxSession session = wxClient.exchangeCode(dto.getCode());
        String openId = session.getOpenid();

        UserWxBind bind = bindMapper.selectOne(Wrappers.<UserWxBind>lambdaQuery()
                .eq(UserWxBind::getWxAppid, wxProperties.getAppId())
                .eq(UserWxBind::getOpenId, openId));

        // 未绑定：不签发令牌，只暂存 openid 供绑定接口校验
        if (bind == null) {
            stringRedisTemplate.opsForValue().set(
                    PENDING_BIND_KEY + openId, openId, PENDING_BIND_MINUTES, TimeUnit.MINUTES);
            WxBindResultVO vo = new WxBindResultVO();
            vo.setBound(false);
            vo.setOpenId(openId);
            vo.setMessage("首次使用请先绑定学号与密码");
            return vo;
        }

        SysUser user = sysUserMapper.selectById(bind.getUserId());
        BusinessException.throwIf(user == null, ResultCode.LOGIN_FAILED, "账号不存在");
        BusinessException.throwIf(!Integer.valueOf(1).equals(user.getStatus()),
                ResultCode.ACCOUNT_DISABLED, "账号已被禁用");
        return buildLoginResult(user);
    }

    /** 绑定系统账号：校验 OpenID 来源、账号密码、唯一索引三重保障 */
    @Transactional(rollbackFor = Exception.class)
    public WxBindResultVO bindAccount(WxBindDTO dto) {
        BusinessException.throwIf(!wxProperties.isEnabled(), ResultCode.WX_SERVICE_UNAVAILABLE);

        // 1. OpenID 必须由本服务签发，且一次性使用（防重放）
        String cacheKey = PENDING_BIND_KEY + dto.getOpenId();
        String verified = stringRedisTemplate.opsForValue().get(cacheKey);
        BusinessException.throwIf(verified == null, ResultCode.WX_BIND_EXPIRED);
        stringRedisTemplate.delete(cacheKey);

        // 2. 校验系统账号凭据，防止他人冒用他人学号
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername().trim()));
        BusinessException.throwIf(user == null, ResultCode.LOGIN_FAILED, "学号或密码错误");
        BusinessException.throwIf(!passwordEncoder.matches(dto.getPassword(), user.getPassword()),
                ResultCode.LOGIN_FAILED, "学号或密码错误");
        BusinessException.throwIf(!Integer.valueOf(1).equals(user.getStatus()),
                ResultCode.ACCOUNT_DISABLED, "账号已被禁用");

        // 3. 写绑定关系，唯一索引兜底并发
        UserWxBind bind = new UserWxBind();
        bind.setUserId(user.getId());
        bind.setWxAppid(wxProperties.getAppId());
        bind.setOpenId(dto.getOpenId());
        try {
            bindMapper.insert(bind);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.WX_ALREADY_BOUND);
        }

        log.info("[微信登录] 绑定成功 userId={}", user.getId());
        return buildLoginResult(user);
    }

    /** 组装登录成功结果 */
    private WxBindResultVO buildLoginResult(SysUser user) {
        String token = tokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole());

        // 令牌写入 Redis 白名单，与密码登录保持一致：
        // 注销时靠删除白名单条目使令牌立即失效
        stringRedisTemplate.opsForValue().set(
                RedisKeys.AUTH_TOKEN + user.getId(), token,
                tokenProvider.getExpireSeconds(), TimeUnit.SECONDS);

        WxBindResultVO vo = new WxBindResultVO();
        vo.setBound(true);
        vo.setUserId(user.getId());
        vo.setToken(token);
        vo.setExpiresIn(tokenProvider.getExpireSeconds());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return vo;
    }
}
