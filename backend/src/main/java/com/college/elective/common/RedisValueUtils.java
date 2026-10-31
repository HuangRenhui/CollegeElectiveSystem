package com.college.elective.common;

/**
 * Redis 字符串值的容错解析。
 *
 * <p><b>为什么需要统一收口</b>：Redis 中的容量、排队号、排队起始时刻等值
 * 均由脚本或运维命令写入，一旦被人工改错、被其它系统写入非法字符串，
 * 直接 {@code Integer.parseInt} / {@code Long.parseLong} 会抛出
 * {@link NumberFormatException}，被全局异常处理器包装成 500。
 * 这类故障「一个脏 key 打挂一个接口」，必须按「脏数据视为缓存缺失」处理，
 * 交由回源逻辑自愈，而不是让请求失败。</p>
 *
 * <p>因此本项目约定：<b>所有从 Redis 读出的数值一律经由本类解析</b>，
 * 解析失败返回 {@code null}（或调用方指定的默认值），绝不抛异常。</p>
 */
public final class RedisValueUtils {

    private RedisValueUtils() {
    }

    /**
     * 解析为整数，失败返回 {@code null}。
     *
     * @param value Redis 原始值，允许为 {@code null} / 空串
     * @return 解析结果；非法输入返回 {@code null}
     */
    public static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析为非负整数，失败或为负时返回 {@code null}。
     *
     * <p>用于「余量」这类语义上不可能为负的值：负值视为脏数据，
     * 交由调用方回源重建，而不是展示成负余量。</p>
     *
     * @param value Redis 原始值
     * @return 解析结果；非法或负数返回 {@code null}
     */
    public static Integer parseNonNegativeInt(String value) {
        Integer parsed = parseInt(value);
        if (parsed == null || parsed < 0) {
            return null;
        }
        return parsed;
    }

    /**
     * 解析为长整数，失败返回 {@code null}。
     *
     * @param value Redis 原始值
     * @return 解析结果；非法输入返回 {@code null}
     */
    public static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析为长整数，失败返回默认值。
     *
     * @param value        Redis 原始值
     * @param defaultValue 解析失败时的兜底值
     * @return 解析结果或默认值
     */
    public static long parseLong(String value, long defaultValue) {
        Long parsed = parseLong(value);
        return parsed == null ? defaultValue : parsed;
    }
}
