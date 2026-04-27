package com.campus.activity.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐电子票务相关表结构，避免本地数据库未执行迁移脚本时直接报错。
 */
@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException ex) {
            log.warn("Skip ticket schema initialization because database connection is unavailable: {}", ex.getMessage());
            return;
        }

        try (connection) {
            DatabaseMetaData metaData = connection.getMetaData();
            ensureColumn(
                    metaData,
                    "activity_registrations",
                    "ticket_code",
                    "ALTER TABLE activity_registrations ADD COLUMN ticket_code VARCHAR(64) NULL AFTER cancelled_at"
            );
            ensureColumn(
                    metaData,
                    "activity_registrations",
                    "ticket_issued_at",
                    "ALTER TABLE activity_registrations ADD COLUMN ticket_issued_at DATETIME NULL AFTER ticket_code"
            );
            ensureColumn(
                    metaData,
                    "activity_registrations",
                    "checkin_operator_id",
                    "ALTER TABLE activity_registrations ADD COLUMN checkin_operator_id BIGINT NULL AFTER checkin_at"
            );
            ensureIndex(
                    metaData,
                    "activity_registrations",
                    "uk_activity_registrations_ticket_code",
                    "CREATE UNIQUE INDEX uk_activity_registrations_ticket_code ON activity_registrations (ticket_code)"
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize ticket schema", ex);
        }
    }

    private void ensureColumn(DatabaseMetaData metaData, String tableName, String columnName, String ddl) throws SQLException {
        if (hasColumn(metaData, tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Initialized missing column {}.{}", tableName, columnName);
    }

    private void ensureIndex(DatabaseMetaData metaData, String tableName, String indexName, String ddl) throws SQLException {
        if (hasIndex(metaData, tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Initialized missing index {} on {}", indexName, tableName);
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, normalizeIdentifier(metaData, tableName), normalizeIdentifier(metaData, columnName))) {
            return columns.next();
        }
    }

    private boolean hasIndex(DatabaseMetaData metaData, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, normalizeIdentifier(metaData, tableName), false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(String.valueOf(currentIndexName))) {
                    return true;
                }
            }
            return false;
        }
    }

    private String normalizeIdentifier(DatabaseMetaData metaData, String identifier) throws SQLException {
        if (metaData.storesLowerCaseIdentifiers()) {
            return identifier.toLowerCase();
        }
        if (metaData.storesUpperCaseIdentifiers()) {
            return identifier.toUpperCase();
        }
        return identifier;
    }
}
