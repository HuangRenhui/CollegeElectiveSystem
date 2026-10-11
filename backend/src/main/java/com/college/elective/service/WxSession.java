package com.college.elective.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信 jscode2session 接口返回结果。
 *
 * <p>微信用 HTTP 200 + {@code errcode} 表达业务错误（而非 HTTP 状态码），
 * 因此本类需同时承载业务错误与成功数据两部分。</p>
 *
 * <p><b>安全提示</b>：{@code sessionKey} 等同于该用户的凭证，
 * 仅可在服务端使用，绝不能返回给小程序。</p>
 */
@Data
public class WxSession {

    @JsonProperty("openid")
    private String openid;

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("unionid")
    private String unionid;

    @JsonProperty("errcode")
    private Integer errcode;

    @JsonProperty("errmsg")
    private String errmsg;
}
