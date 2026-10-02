package com.college.elective.common;

/**
 * 分页参数归一化，避免客户端传入超大 pageSize 一次打满内存。
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
}
