package com.college.elective;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.college.elective.common.Pages;
import com.college.elective.common.RedisValueUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 分页参数归一化与 Redis 数值容错解析的单元测试。
 *
 * <p>这两处都是「脏输入防护」的关键路径：分页参数决定了一次 SQL 会拉回多少行，
 * Redis 数值解析决定了脏 key 会不会把接口打成 500。二者都是纯逻辑，适合用单测锁死行为。</p>
 */
class PaginationAndCacheValueTests {

    // ---------------------------- Pages ----------------------------

    @Test
    @DisplayName("页码非法时归一化为 1")
    void pageNumInvalidFallsBackToOne() {
        assertEquals(1L, Pages.pageNum(null));
        assertEquals(1L, Pages.pageNum(0L));
        assertEquals(1L, Pages.pageNum(-10L));
        assertEquals(3L, Pages.pageNum(3L));
    }

    @Test
    @DisplayName("每页条数非法时归一化为默认值，超限时截断为上限")
    void pageSizeIsClamped() {
        assertEquals(Pages.DEFAULT_SIZE, Pages.pageSize(null));
        assertEquals(Pages.DEFAULT_SIZE, Pages.pageSize(-1L));
        assertEquals(Pages.DEFAULT_SIZE, Pages.pageSize(0L));
        assertEquals(20L, Pages.pageSize(20L));
        assertEquals(Pages.MAX_SIZE, Pages.pageSize(100_000L));
    }

    @Test
    @DisplayName("分页对象被就地纠正：超大 size 与负数 offset 都不会直达 SQL")
    void clampFixesPageObject() {
        Page<String> page = new Page<>(0, 100_000);
        Pages.clamp(page);
        assertEquals(1L, page.getCurrent(), "页码 0 会拼出负数 offset，必须纠正");
        assertEquals(Pages.MAX_SIZE, page.getSize(), "超大 pageSize 必须截断");
    }

    @Test
    @DisplayName("size 为负会被纠正为正数（否则 MyBatis-Plus 不拼 LIMIT，等同全表扫描）")
    void clampFixesNegativeSize() {
        Page<String> page = new Page<>(1, -1);
        Pages.clamp(page);
        assertEquals(Pages.DEFAULT_SIZE, page.getSize());
    }

    @Test
    @DisplayName("对 null 分页对象安全返回")
    void clampNullIsSafe() {
        assertNull(Pages.clamp(null));
    }

    // ---------------------------- RedisValueUtils ----------------------------

    @Test
    @DisplayName("合法数值正常解析并去除空白")
    void parseIntAndLong() {
        assertEquals(15, RedisValueUtils.parseInt("15").intValue());
        assertEquals(15, RedisValueUtils.parseInt(" 15 ").intValue());
        assertEquals(900L, RedisValueUtils.parseLong("900").longValue());
    }

    @Test
    @DisplayName("空值与脏值一律返回 null，不抛异常")
    void dirtyValuesReturnNull() {
        assertNull(RedisValueUtils.parseInt(null));
        assertNull(RedisValueUtils.parseInt(""));
        assertNull(RedisValueUtils.parseInt("abc"));
        assertNull(RedisValueUtils.parseInt("1.5"));
        assertNull(RedisValueUtils.parseLong("not-a-number"));
    }

    @Test
    @DisplayName("余量类字段拒绝负数：负余量视为脏数据，交由回源重建")
    void nonNegativeIntRejectsNegative() {
        assertNull(RedisValueUtils.parseNonNegativeInt("-1"));
        assertEquals(0, RedisValueUtils.parseNonNegativeInt("0").intValue());
        assertEquals(8, RedisValueUtils.parseNonNegativeInt("8").intValue());
    }

    @Test
    @DisplayName("带默认值的解析在脏值上回退到默认值")
    void parseLongWithDefault() {
        assertEquals(777L, RedisValueUtils.parseLong("abc", 777L));
        assertEquals(5L, RedisValueUtils.parseLong("5", 777L));
    }
}
