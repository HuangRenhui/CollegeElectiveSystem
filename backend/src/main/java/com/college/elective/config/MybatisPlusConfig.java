package com.college.elective.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ParameterUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.college.elective.common.Pages;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：分页、乐观锁、防全表更新删除、字段自动填充。
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（统一收敛分页参数，详见 clampPagination）
        interceptor.addInnerInterceptor(clampPagination());
        // 乐观锁插件（course.version）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止恶意的全表更新与删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 分页插件：在拦截器层统一归一化所有分页参数。
     *
     * <p><b>为什么必须放在拦截器层而不是各个 Service</b>：
     * 本项目的列表接口散落在十余个 Service 中，逐个调用 {@link Pages#pageSize(Long)}
     * 既容易漏写（新增入口时无人提醒），也无法约束 Mapper 直接接收 {@code IPage} 的写法。
     * 放在分页插件里，所有走 MyBatis-Plus 分页的查询都会被强制收敛。</p>
     *
     * <p><b>两个必须拦截的输入</b>：</p>
     * <ul>
     *   <li>{@code size > MAX_SIZE}：一次拉走整表，打满内存；</li>
     *   <li>{@code size < 0}：MyBatis-Plus 会视为「不分页」而不拼 LIMIT，
     *       等价于绕过所有限流直接全表扫描；{@code current < 1} 则会拼出负 offset
     *       导致 SQL 语法错误（500）。</li>
     * </ul>
     *
     * <p>{@code willDoQuery} 里也要归一化：它在 {@code beforeQuery} 之前执行，
     * 若此时 {@code size} 仍为负数，插件会跳过 count 查询，导致返回的
     * {@code total} 为 0 而 {@code records} 非空，前端分页器将显示错误页数。</p>
     */
    private PaginationInnerInterceptor clampPagination() {
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL) {
            @Override
            public boolean willDoQuery(Executor executor, MappedStatement ms, Object parameter,
                                       RowBounds rowBounds, ResultHandler resultHandler,
                                       BoundSql boundSql) throws SQLException {
                clampPage(parameter);
                return super.willDoQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            }

            @Override
            public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                                    RowBounds rowBounds, ResultHandler resultHandler,
                                    BoundSql boundSql) throws SQLException {
                clampPage(parameter);
                super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            }
        };
        // 兜底：即使 clampPage 未生效（例如分页对象以自定义类型传入），
        // 插件自身也会把超过上限的 size 归为上限值
        pagination.setMaxLimit(Pages.MAX_SIZE);
        pagination.setOverflow(false);
        return pagination;
    }

    /**
     * 从 Mapper 方法入参中取出分页对象并就地纠正页码与每页条数。
     */
    private void clampPage(Object parameter) {
        IPage<?> page = ParameterUtils.findPage(parameter).orElse(null);
        Pages.clamp(page);
    }

    /**
     * 公共字段自动填充：createTime / updateTime。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
