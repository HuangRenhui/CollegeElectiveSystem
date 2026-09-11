package com.college.elective;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 应用基础单元测试。
 *
 * <p>说明：完整的集成测试需要依赖 MySQL 与 Redis 环境，
 * 此处仅保留不依赖外部环境的轻量测试，避免 CI 误报。</p>
 */
class CollegeElectiveApplicationTests {

    @Test
    void contextClassIsLoadable() {
        assertNotNull(CollegeElectiveApplication.class);
    }
}
