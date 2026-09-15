package com.college.elective.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库表结构自动初始化组件。
 *
 * <p>开发环境下应用启动时，自动检测缺失的数据表并创建，避免手工执行建表脚本。
 * 生产环境（{@code prod}）不启用，防止误操作。</p>
 *
 * <h3>工作方式</h3>
 * <ol>
 *   <li>从 {@code db/schema.sql} 读取建表语句；</li>
 *   <li>过滤掉库级语句（{@code CREATE DATABASE}、{@code USE}）与破坏性语句（{@code DROP TABLE}）；</li>
 *   <li>将 {@code CREATE TABLE} 改写为 {@code CREATE TABLE IF NOT EXISTS}，保证幂等；</li>
 *   <li>检查每张表是否已存在，仅执行缺失表的建表语句。</li>
 * </ol>
 *
 * <h3>安全保障</h3>
 * <ul>
 *   <li><b>不删数据</b>：脚本中的 {@code DROP TABLE} 会被过滤，已存在的表不会被重建或修改；</li>
 *   <li><b>幂等可重入</b>：重复启动不会产生任何副作用；</li>
 *   <li><b>失败不阻断</b>：初始化异常仅记录日志，不影响应用启动。</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <pre>{@code
 * elective:
 *   database:
 *     auto-init: true      # 是否启用自动建表（默认 dev 开启）
 * }</pre>
 *
 * <p>如需关闭自动建表，可将该项设为 {@code false}，或使用 {@code prod} 环境启动。</p>
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DatabaseInitializer {

    /** 建表脚本位置 */
    private static final String SCHEMA_LOCATION = "db/schema.sql";

    private final DataSource dataSource;
    private final Environment environment;

    /**
     * 应用启动完成后执行表结构检查与初始化。
     *
     * <p>使用 {@link ApplicationReadyEvent} 而非 {@code @PostConstruct}，
     * 确保数据源与连接池已完全就绪。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        if (!isAutoInitEnabled()) {
            log.debug("数据库自动初始化已关闭，跳过表结构检查");
            return;
        }

        log.info("开始检查数据库表结构...");
        long start = System.currentTimeMillis();

        try {
            List<TableDefinition> definitions = loadTableDefinitions();
            if (definitions.isEmpty()) {
                log.warn("未从 {} 解析到任何建表语句，请检查脚本内容", SCHEMA_LOCATION);
                return;
            }

            try (Connection connection = dataSource.getConnection()) {
                List<TableDefinition> missing = findMissingTables(connection, definitions);

                if (missing.isEmpty()) {
                    log.info("数据库表结构检查完成，{} 张表均已存在，无需初始化（耗时 {} ms）",
                            definitions.size(), System.currentTimeMillis() - start);
                    return;
                }

                log.info("检测到 {} 张缺失数据表，开始自动创建：{}",
                        missing.size(), missing.stream().map(d -> d.tableName).toList());

                int created = executeCreateStatements(connection, missing);

                log.info("数据库表结构初始化完成，成功创建 {} 张表（耗时 {} ms）",
                        created, System.currentTimeMillis() - start);

                if (created < missing.size()) {
                    log.warn("有 {} 张表创建失败，请检查上方错误日志",
                            missing.size() - created);
                }
            }
        } catch (Exception e) {
            // 初始化失败不应阻断应用启动，仅提示开发者
            log.error("数据库表结构自动初始化失败，应用将继续启动。"
                    + "请检查数据库连接与脚本内容，或手工执行 db/schema.sql。原因：{}", e.getMessage());
            log.debug("初始化异常详情", e);
        }
    }

    /**
     * 判断是否启用自动建表。
     *
     * <p>默认在非 prod 环境开启；可通过 {@code elective.database.auto-init} 显式控制。</p>
     */
    private boolean isAutoInitEnabled() {
        String configured = environment.getProperty("elective.database.auto-init");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        // 未显式配置时，默认开启（因本类已在 prod 环境被排除）
        return true;
    }

    /**
     * 从 classpath 读取并解析建表脚本。
     */
    private List<TableDefinition> loadTableDefinitions() throws Exception {
        ClassPathResource resource = new ClassPathResource(SCHEMA_LOCATION);
        if (!resource.exists()) {
            log.warn("未找到建表脚本 {}，跳过自动初始化", SCHEMA_LOCATION);
            return List.of();
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return parseTableDefinitions(content.toString());
    }

    /**
     * 解析建表语句，提取「表名 + 建表 SQL」。
     *
     * <p>会过滤库级语句与破坏性语句，并将建表语句改写为幂等形式。</p>
     */
    private List<TableDefinition> parseTableDefinitions(String sql) {
        List<TableDefinition> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentTable = null;
        boolean inTable = false;

        for (String rawLine : sql.split("\n")) {
            String line = rawLine.trim();

            // 跳过空行与注释
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("#")) {
                continue;
            }

            // 过滤库级与破坏性语句
            if (line.toUpperCase().startsWith("CREATE DATABASE")
                    || line.toUpperCase().startsWith("USE ")
                    || line.toUpperCase().startsWith("DROP TABLE")
                    || line.toUpperCase().startsWith("SET ")) {
                continue;
            }

            // 建表语句开始
            if (line.toUpperCase().startsWith("CREATE TABLE")) {
                currentTable = extractTableName(line);
                if (currentTable == null) {
                    continue;
                }
                inTable = true;
                // 改写为幂等形式：已存在则跳过
                String rewritten = rewriteToIfNotExists(line);
                current.append(rewritten).append('\n');
                continue;
            }

            if (inTable) {
                current.append(rawLine).append('\n');
                if (line.endsWith(";")) {
                    result.add(new TableDefinition(currentTable, current.toString().trim()));
                    current.setLength(0);
                    currentTable = null;
                    inTable = false;
                }
            }
        }

        return result;
    }

    /**
     * 从 {@code CREATE TABLE `xxx`} 中提取表名。
     */
    private String extractTableName(String line) {
        int start = line.indexOf('`');
        if (start < 0) {
            return null;
        }
        int end = line.indexOf('`', start + 1);
        if (end < 0) {
            return null;
        }
        return line.substring(start + 1, end);
    }

    /**
     * 将 {@code CREATE TABLE `xxx`} 改写为 {@code CREATE TABLE IF NOT EXISTS `xxx`}。
     */
    private String rewriteToIfNotExists(String line) {
        String upper = line.toUpperCase();
        if (upper.contains("IF NOT EXISTS")) {
            return line;
        }
        return line.replaceFirst("(?i)CREATE\\s+TABLE\\s+", "CREATE TABLE IF NOT EXISTS ");
    }

    /**
     * 查询数据库，筛出尚未创建的表。
     */
    private List<TableDefinition> findMissingTables(Connection connection,
                                                    List<TableDefinition> definitions) throws SQLException {
        Map<String, Boolean> existCache = new LinkedHashMap<>();
        List<TableDefinition> missing = new ArrayList<>();

        for (TableDefinition definition : definitions) {
            Boolean exists = existCache.get(definition.tableName);
            if (exists == null) {
                exists = tableExists(connection, definition.tableName);
                existCache.put(definition.tableName, exists);
            }
            if (!exists) {
                missing.add(definition);
            }
        }
        return missing;
    }

    /**
     * 判断指定表是否存在。
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        // 优先使用当前 catalog（即连接的数据库）
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, tableName,
                new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        // 兼容大小写敏感场景：再按大写/小写各查一次
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null,
                tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null,
                tableName.toLowerCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * 逐条执行缺失表的建表语句。
     *
     * @return 成功创建的表数量
     */
    private int executeCreateStatements(Connection connection,
                                        List<TableDefinition> missing) {
        int success = 0;
        for (TableDefinition definition : missing) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(definition.sql);
                success++;
                log.info("  创建数据表 [{}] 成功", definition.tableName);
            } catch (SQLException e) {
                // 并发启动场景下，另一实例可能已创建该表，此处不算失败
                if (isAlreadyExistsError(e)) {
                    success++;
                    log.info("  数据表 [{}] 已由其他实例创建，跳过", definition.tableName);
                } else {
                    log.error("  创建数据表 [{}] 失败：{}", definition.tableName, e.getMessage());
                    log.debug("建表语句执行异常详情", e);
                }
            }
        }
        return success;
    }

    /**
     * 判断是否为「表已存在」错误（MySQL 错误码 1050）。
     */
    private boolean isAlreadyExistsError(SQLException e) {
        return e.getErrorCode() == 1050
                || (e.getMessage() != null && e.getMessage().contains("already exists"));
    }

    /**
     * 表定义：表名 + 建表 SQL。
     */
    private record TableDefinition(String tableName, String sql) {
    }
}
