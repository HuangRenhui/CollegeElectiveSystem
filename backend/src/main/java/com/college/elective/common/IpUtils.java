package com.college.elective.common;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具。
 *
 * <p>系统在部署时通常会经过 Nginx 等反向代理，此时 {@code request.getRemoteAddr()}
 * 拿到的是代理服务器的地址，而非真实客户端 IP。因此需要优先从代理写入的请求头中读取。</p>
 *
 * <p>解析优先级：</p>
 * <ol>
 *   <li>{@code X-Forwarded-For} —— 标准反向代理头，经过多层代理时形如
 *       {@code 客户端IP, 代理1IP, 代理2IP}，取第一段为客户端真实 IP；</li>
 *   <li>{@code X-Real-IP} —— Nginx 常用头，仅配置了该头时使用；</li>
 *   <li>{@code request.getRemoteAddr()} —— 直连场景，无代理头时兜底。</li>
 * </ol>
 *
 * <p>注意：代理头由客户端可伪造，本工具仅用于日志记录与风控参考，
 * 不应作为安全鉴权的唯一依据。</p>
 *
 * @see <a href="https://developer.mozilla.org/docs/Web/HTTP/Headers/X-Forwarded-For">X-Forwarded-For</a>
 */
public final class IpUtils {

    /** 代理未获取到真实 IP 时写入的占位值 */
    private static final String UNKNOWN = "unknown";

    /** IPv6 回环地址 */
    private static final String IPV6_LOOPBACK = "0:0:0:0:0:0:0:1";

    /** IPv4 回环地址 */
    private static final String IPV4_LOOPBACK = "127.0.0.1";

    private IpUtils() {
        // 工具类禁止实例化
    }

    /**
     * 解析请求的真实客户端 IP。
     *
     * @param request 当前 HTTP 请求，允许为 {@code null}
     * @return 客户端 IP；请求为 {@code null} 时返回 {@code null}，
     *         IPv6 回环地址会被归一化为 {@code 127.0.0.1}
     */
    public static String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 优先取标准转发头，多层代理时取第一段（即最初的客户端）
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }

        // 形如 "客户端IP, 代理1IP" 时取第一段
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 本机访问时 Java 可能返回 IPv6 回环地址，统一归一化便于阅读与统计
        return IPV6_LOOPBACK.equals(ip) ? IPV4_LOOPBACK : ip;
    }

    /**
     * 判断解析结果是否无效（为空或代理写入的 unknown 占位值）。
     */
    private static boolean isInvalid(String ip) {
        return StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip);
    }
}
