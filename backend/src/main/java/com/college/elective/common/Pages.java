package com.college.elective.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 分页参数归一化，避免客户端传入超大 pageSize 一次打满内存。
 *
 * <p>本类有两个使用层次：</p>
 * <ul>
 *   <li>{@link #pageNum(Long)} / {@link #pageSize(Long)}：供业务代码在构造
 *       {@code Page} 前显式归一化；</li>
 *   <li>{@link #clamp(IPage)}：由 MyBatis-Plus 分页插件统一调用，
 *       覆盖所有未经显式归一化的入口（新增 Mapper/Service 时无需再记得写一遍），
 *       防止 {@code pageSize=100000} 或 {@code pageNum=-1} 直达 SQL。</li>
 * </ul>
 */
public final class Pages {

    private Pages() {
    }

    public static final long DEFAULT_SIZE = 10L;
    public static final long MAX_SIZE = 100L;

    public static long pageNum(Long pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1L;
        }
        return pageNum;
    }

    public static long pageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(pageSize, MAX_SIZE);
    }

    /**
     * 就地纠正分页对象中的页码与每页条数。
     *
     * <p><b>为什么需要它</b>：MyBatis-Plus 对 {@code size < 0} 的处理是「不构造分页 SQL」，
     * 即一次 {@code pageSize=-1} 的请求可拉回整张表；而 {@code current < 1} 会拼出
     * 负数 offset 直接导致 SQL 报错。两处都属于「客户端输入直达数据库」，
     * 必须在拦截器层统一收敛。</p>
     *
     * @param page 分页对象，允许为 {@code null}
     * @param <T>  记录类型
     * @return 原对象（便于链式调用）；入参为 {@code null} 时返回 {@code null}
     */
    public static <T> IPage<T> clamp(IPage<T> page) {
        if (page == null) {
            return null;
        }
        long current = pageNum(page.getCurrent());
        long size = pageSize(page.getSize());
        if (current != page.getCurrent()) {
            page.setCurrent(current);
        }
        if (size != page.getSize()) {
            page.setSize(size);
        }
        return page;
    }
}
