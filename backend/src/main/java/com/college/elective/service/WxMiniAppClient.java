package com.college.elective.service;

import cn.hutool.core.util.StrUtil;
import com.college.elective.common.BusinessException;
import com.college.elective.common.ResultCode;
import com.college.elective.config.WxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信小程序 API 客户端。
 *
 * <p><b>安全要点</b>：</p>
 * <ol>
 *   <li>{@code session_key} 等同于该用户的凭证，<b>绝不能返回给小程序</b>；</li>
 *   <li>AppSecret 只在服务端使用，绝不能出现在任何前端代码或配置仓库中；</li>
 *   <li>{@code code} 一次性且有效期 5 分钟，可防重放。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxMiniAppClient {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    private static final String SUBSCRIBE_SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token";

    /** 微信成功标志：响应体中含 errcode 0 */
    private static final String ERRCODE_ZERO = "\"errcode\":0";

    private final WxProperties properties;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 用小程序临时登录凭证换取 OpenID。
     *
     * @param code wx.login() 得到的临时登录凭证
     * @return openid 与 session_key
     * @throws BusinessException 微信返回错误或服务不可用
     */
    public WxSession exchangeCode(String code) {
        String url = String.format(
                "%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                JSCODE2SESSION_URL, properties.getAppId(), properties.getAppSecret(), code);

        WxSession session;
        try {
            String resp = restTemplate.getForObject(url, String.class);
            session = objectMapper.readValue(resp, WxSession.class);
        } catch (RestClientException e) {
            log.error("[微信] 请求 jscode2session 异常", e);
            throw new BusinessException(ResultCode.WX_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("[微信] 解析 jscode2session 响应失败", e);
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }

        // 微信用 errcode 表达业务错误，HTTP 状态仍是 200
        if (session.getErrcode() != null && session.getErrcode() != 0) {
            log.warn("[微信] jscode2session 失败 errcode={}, errmsg={}",
                    session.getErrcode(), session.getErrmsg());
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        if (StrUtil.isBlank(session.getOpenid())) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED);
        }
        return session;
    }

    /**
     * 发送订阅消息。
     *
     * <p><b>注意</b>：微信侧的次数扣减是隐式的，本方法不负责扣减，
     * 需由调用方在数据库中先行扣减（宁可漏发不可超发）。</p>
     *
     * @param openId     接收者 OpenID
     * @param templateId 模板ID
     * @param data       模板字段键值对
     * @return true 发送成功
     */
    public boolean sendSubscribeMessage(String openId, String templateId,
                                        Map<String, Object> data) {
        Map<String, Object> payload = Map.of(
                "touser", openId,
                "template_id", templateId,
                "page", "pages/index/index",
                "data", data);

        try {
            String accessToken = getAccessToken();
            String url = SUBSCRIBE_SEND_URL + "?access_token=" + accessToken;
            String resp = restTemplate.postForObject(url, payload, String.class);
            // errcode 为 0 表示成功；43001 等错误码表示订阅次数已用尽
            return resp != null && resp.contains(ERRCODE_ZERO);
        } catch (Exception e) {
            log.warn("[微信] 发送订阅消息失败 openId={}, templateId={}",
                    openId, templateId, e);
            return false;
        }
    }

    /**
     * 获取接口调用凭证。
     *
     * <p>access_token 有效期 2 小时，这里每次调用都重新获取以保证正确性。
     * 多实例部署时各实例独立请求，属可接受的权衡——
     * 多刷几次不会导致功能异常，只是多几次网络请求。</p>
     */
    private String getAccessToken() {
        String url = String.format(
                "%s?grant_type=client_credential&appid=%s&secret=%s",
                TOKEN_URL, properties.getAppId(), properties.getAppSecret());
        try {
            String resp = restTemplate.getForObject(url, String.class);
            Map<?, ?> map = objectMapper.readValue(resp, Map.class);
            Object token = map.get("access_token");
            return token == null ? "" : token.toString();
        } catch (Exception e) {
            log.error("[微信] 获取 access_token 失败", e);
            throw new BusinessException(ResultCode.WX_SERVICE_UNAVAILABLE);
        }
    }
}
