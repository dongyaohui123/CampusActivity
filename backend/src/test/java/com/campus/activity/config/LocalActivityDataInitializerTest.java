package com.campus.activity.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class LocalActivityDataInitializerTest {

    @Test
    void run_shouldSkipWhenConnectionUnavailable() throws Exception {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        LocalActivityDataInitializer initializer = new LocalActivityDataInitializer(
                unavailableDataSource(new SQLException("down")),
                jdbcTemplate
        );

        initializer.run(null);

        assertThat(jdbcTemplate.updateCalls).isEmpty();
    }

    @Test
    void run_shouldSkipWhenDatabaseIsNotLocal() throws Exception {
        ConnectionHandle handle = createConnectionHandle("jdbc:mysql://db.example.com:3306/campus_activity_v2");
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        LocalActivityDataInitializer initializer = new LocalActivityDataInitializer(
                fixedDataSource(handle.connection),
                jdbcTemplate
        );

        initializer.run(null);

        assertThat(jdbcTemplate.updateCalls).isEmpty();
        assertThat(handle.closed).isTrue();
    }

    @Test
    void run_shouldRenameVenuesAndInsertDemoActivityForLocalDatabase() throws Exception {
        ConnectionHandle handle = createConnectionHandle("jdbc:mysql://localhost:3306/campus_activity_v2");
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_ORGANIZER_SQL, 11L);
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_DEMO_ACTIVITY_SQL, new EmptyResultDataAccessException(1));
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_DEMO_ACTIVITY_SQL, 101L);
        jdbcTemplate.addUpdateResult(LocalActivityDataInitializer.UPDATE_DEMO_REVIEW_SQL, 0);

        LocalActivityDataInitializer initializer = new LocalActivityDataInitializer(
                fixedDataSource(handle.connection),
                jdbcTemplate
        );

        initializer.run(null);

        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.RENAME_LOCATION_SQL)).isEqualTo(2);
        UpdateCall insertActivityCall = jdbcTemplate.findSingleUpdate(LocalActivityDataInitializer.INSERT_DEMO_ACTIVITY_SQL);
        assertThat(insertActivityCall).isNotNull();
        assertThat(insertActivityCall.args[0]).isEqualTo(11L);
        assertThat(insertActivityCall.args[1]).isEqualTo(11L);
        assertThat(insertActivityCall.args[2]).isEqualTo(LocalActivityDataInitializer.DEMO_TITLE);
        assertThat(insertActivityCall.args[5]).isEqualTo(LocalActivityDataInitializer.DEMO_LOCATION);
        assertThat(insertActivityCall.args[11]).isEqualTo("PUBLISHED");
        assertThat(insertActivityCall.args[12]).isEqualTo("PUBLIC");
        assertThat(insertActivityCall.args[13]).isEqualTo(false);
        assertThat(insertActivityCall.args[6]).isInstanceOf(LocalDateTime.class);
        assertThat(insertActivityCall.args[7]).isInstanceOf(LocalDateTime.class);
        assertThat(insertActivityCall.args[8]).isInstanceOf(LocalDateTime.class);

        UpdateCall insertReviewCall = jdbcTemplate.findSingleUpdate(LocalActivityDataInitializer.INSERT_DEMO_REVIEW_SQL);
        assertThat(insertReviewCall).isNotNull();
        assertThat(insertReviewCall.args[0]).isEqualTo(101L);
        assertThat(insertReviewCall.args[1]).isEqualTo("APPROVED");
        assertThat(insertReviewCall.args[2]).isEqualTo(LocalActivityDataInitializer.DEMO_REVIEW_COMMENT);
    }

    @Test
    void run_shouldUpdateExistingDemoActivityWithoutInsertingDuplicate() throws Exception {
        ConnectionHandle handle = createConnectionHandle("jdbc:mysql://127.0.0.1:3306/campus_activity_v2");
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_ORGANIZER_SQL, 11L);
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_DEMO_ACTIVITY_SQL, 88L);
        jdbcTemplate.addUpdateResult(LocalActivityDataInitializer.UPDATE_DEMO_REVIEW_SQL, 1);

        LocalActivityDataInitializer initializer = new LocalActivityDataInitializer(
                fixedDataSource(handle.connection),
                jdbcTemplate
        );

        initializer.run(null);

        UpdateCall updateActivityCall = jdbcTemplate.findSingleUpdate(LocalActivityDataInitializer.UPDATE_DEMO_ACTIVITY_SQL);
        assertThat(updateActivityCall).isNotNull();
        assertThat(updateActivityCall.args[0]).isEqualTo(11L);
        assertThat(updateActivityCall.args[1]).isEqualTo(11L);
        assertThat(updateActivityCall.args[2]).isEqualTo(LocalActivityDataInitializer.DEMO_SUMMARY);
        assertThat(updateActivityCall.args[3]).isEqualTo(LocalActivityDataInitializer.DEMO_CONTENT);
        assertThat(updateActivityCall.args[8]).isEqualTo("PUBLISHED");
        assertThat(updateActivityCall.args[9]).isEqualTo("PUBLIC");
        assertThat(updateActivityCall.args[10]).isEqualTo(false);
        assertThat(updateActivityCall.args[13]).isEqualTo(88L);
        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.INSERT_DEMO_ACTIVITY_SQL)).isZero();
        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.INSERT_DEMO_REVIEW_SQL)).isZero();
    }

    @Test
    void run_shouldSkipDemoInitializationWhenNoOrganizerExists() throws Exception {
        ConnectionHandle handle = createConnectionHandle("jdbc:mysql://localhost:3306/campus_activity_v2");
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        jdbcTemplate.addQueryResult(LocalActivityDataInitializer.FIND_ORGANIZER_SQL, new EmptyResultDataAccessException(1));

        LocalActivityDataInitializer initializer = new LocalActivityDataInitializer(
                fixedDataSource(handle.connection),
                jdbcTemplate
        );

        initializer.run(null);

        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.RENAME_LOCATION_SQL)).isEqualTo(2);
        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.INSERT_DEMO_ACTIVITY_SQL)).isZero();
        assertThat(jdbcTemplate.countUpdates(LocalActivityDataInitializer.UPDATE_DEMO_ACTIVITY_SQL)).isZero();
        assertThat(jdbcTemplate.queryCallsFor(LocalActivityDataInitializer.FIND_DEMO_ACTIVITY_SQL)).isZero();
    }

    private static DataSource fixedDataSource(Connection connection) {
        return new DataSource() {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public Connection getConnection(String username, String password) {
                return connection;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("unwrap not supported");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public Logger getParentLogger() {
                return Logger.getGlobal();
            }
        };
    }

    private static DataSource unavailableDataSource(SQLException exception) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                throw exception;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw exception;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("unwrap not supported");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public Logger getParentLogger() {
                return Logger.getGlobal();
            }
        };
    }

    private static ConnectionHandle createConnectionHandle(String jdbcUrl) {
        ConnectionHandle handle = new ConnectionHandle();
        DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    if ("getURL".equals(method.getName())) {
                        return jdbcUrl;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        return metaData;
                    }
                    if ("close".equals(method.getName())) {
                        handle.closed = true;
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return handle.closed;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        handle.connection = connection;
        return handle;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class ConnectionHandle {
        private Connection connection;
        private boolean closed;
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final Map<String, Deque<Object>> queryResults = new HashMap<>();
        private final Map<String, Deque<Integer>> updateResults = new HashMap<>();
        private final List<UpdateCall> updateCalls = new ArrayList<>();
        private final Map<String, Integer> queryCounts = new HashMap<>();

        void addQueryResult(String sql, Object result) {
            queryResults.computeIfAbsent(sql, key -> new ArrayDeque<>()).addLast(result);
        }

        void addUpdateResult(String sql, int result) {
            updateResults.computeIfAbsent(sql, key -> new ArrayDeque<>()).addLast(result);
        }

        int countUpdates(String sql) {
            return (int) updateCalls.stream().filter(call -> Objects.equals(call.sql, sql)).count();
        }

        int queryCallsFor(String sql) {
            return queryCounts.getOrDefault(sql, 0);
        }

        UpdateCall findSingleUpdate(String sql) {
            return updateCalls.stream().filter(call -> Objects.equals(call.sql, sql)).findFirst().orElse(null);
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            queryCounts.merge(sql, 1, Integer::sum);
            Deque<Object> queue = queryResults.get(sql);
            if (queue == null || queue.isEmpty()) {
                throw new AssertionError("Unexpected queryForObject SQL: " + sql);
            }
            Object result = queue.removeFirst();
            if (result instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return requiredType.cast(result);
        }

        @Override
        public int update(String sql, Object... args) {
            updateCalls.add(new UpdateCall(sql, args));
            Deque<Integer> queue = updateResults.get(sql);
            if (queue == null || queue.isEmpty()) {
                return 1;
            }
            return queue.removeFirst();
        }
    }

    private static final class UpdateCall {
        private final String sql;
        private final Object[] args;

        private UpdateCall(String sql, Object[] args) {
            this.sql = sql;
            this.args = Arrays.copyOf(args, args.length);
        }
    }
}
