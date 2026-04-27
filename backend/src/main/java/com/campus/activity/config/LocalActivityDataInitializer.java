package com.campus.activity.config;

import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.enums.Visibility;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时修正本地演示库中的活动地点数据，并补一条线上演示活动。
 */
@Component
public class LocalActivityDataInitializer implements ApplicationRunner {
    static final String DEMO_TITLE = "线上经验分享会";
    static final String DEMO_LOCATION = "线上";
    static final String DEMO_SUMMARY = "远程参与的线上经验分享活动，报名后按说明加入。";
    static final String DEMO_CONTENT = "本场活动在线上进行，报名成功后可根据活动说明按时参加。";
    static final String DEMO_REVIEW_COMMENT = "本地演示数据初始化";
    static final int DEMO_MAX_PARTICIPANTS = 200;

    static final String RENAME_LOCATION_SQL = "UPDATE activities SET location = ? WHERE location = ?";
    static final String FIND_ORGANIZER_SQL = """
            SELECT u.id
            FROM users u
            INNER JOIN organizer_profiles op ON op.user_id = u.id
            WHERE u.role = ?
              AND u.status = ?
            ORDER BY u.id
            LIMIT 1
            """;
    static final String FIND_DEMO_ACTIVITY_SQL = """
            SELECT id
            FROM activities
            WHERE title = ?
              AND location = ?
            ORDER BY id
            LIMIT 1
            """;
    static final String INSERT_DEMO_ACTIVITY_SQL = """
            INSERT INTO activities (
                organizer_id,
                publisher_id,
                title,
                summary,
                content,
                location,
                start_time,
                end_time,
                registration_deadline,
                max_participants,
                registered_count,
                status,
                visibility,
                is_featured,
                published_at,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    static final String UPDATE_DEMO_ACTIVITY_SQL = """
            UPDATE activities
            SET organizer_id = ?,
                publisher_id = ?,
                summary = ?,
                content = ?,
                start_time = ?,
                end_time = ?,
                registration_deadline = ?,
                max_participants = ?,
                registered_count = COALESCE(registered_count, 0),
                status = ?,
                visibility = ?,
                is_featured = ?,
                published_at = COALESCE(published_at, ?),
                updated_at = ?
            WHERE id = ?
            """;
    static final String UPDATE_DEMO_REVIEW_SQL = """
            UPDATE activity_reviews
            SET review_status = ?,
                reviewer_id = NULL,
                review_comment = ?,
                reviewed_at = ?,
                updated_at = ?
            WHERE activity_id = ?
            """;
    static final String INSERT_DEMO_REVIEW_SQL = """
            INSERT INTO activity_reviews (
                activity_id,
                review_status,
                reviewer_id,
                review_comment,
                reviewed_at,
                created_at,
                updated_at
            ) VALUES (?, ?, NULL, ?, ?, ?, ?)
            """;

    private static final Logger log = LoggerFactory.getLogger(LocalActivityDataInitializer.class);
    private static final String LOCALHOST_TOKEN = "//localhost";
    private static final String LOOPBACK_TOKEN = "//127.0.0.1";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LocalActivityDataInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException ex) {
            log.warn("Skip local activity data initialization because database connection is unavailable: {}", ex.getMessage());
            return;
        }

        try (connection) {
            DatabaseMetaData metaData = connection.getMetaData();
            String jdbcUrl = metaData.getURL();
            if (!isLocalDatabaseUrl(jdbcUrl)) {
                log.info("Skip local activity data initialization for non-local database: {}", jdbcUrl);
                return;
            }

            patchVenueNames();
            Long organizerUserId = findFirstOrganizerUserId();
            if (organizerUserId == null) {
                log.info("Skip online demo activity initialization because no active organizer with profile exists");
                return;
            }

            Long demoActivityId = upsertDemoActivity(organizerUserId);
            ensureApprovedReview(demoActivityId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize local activity demo data", ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to initialize local activity demo data", ex);
        }
    }

    private boolean isLocalDatabaseUrl(String jdbcUrl) {
        String normalized = String.valueOf(jdbcUrl).toLowerCase(Locale.ROOT);
        return normalized.contains(LOCALHOST_TOKEN) || normalized.contains(LOOPBACK_TOKEN);
    }

    private void patchVenueNames() {
        int libraryRenamed = jdbcTemplate.update(RENAME_LOCATION_SQL, "大学生活动中心", "图书馆报告厅");
        int hallRenamed = jdbcTemplate.update(RENAME_LOCATION_SQL, "操场", "大礼堂");
        if (libraryRenamed > 0 || hallRenamed > 0) {
            log.info(
                    "Patched local activity venues: 图书馆报告厅 -> 大学生活动中心 ({}), 大礼堂 -> 操场 ({})",
                    libraryRenamed,
                    hallRenamed
            );
        }
    }

    private Long findFirstOrganizerUserId() {
        try {
            return jdbcTemplate.queryForObject(
                    FIND_ORGANIZER_SQL,
                    Long.class,
                    UserRole.ORGANIZER.name(),
                    UserStatus.ACTIVE.name()
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long upsertDemoActivity(Long organizerUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusDays(7);
        LocalDateTime endTime = startTime.plusHours(2);
        LocalDateTime deadline = startTime.minusDays(1);

        Long existingActivityId = findDemoActivityId();
        if (existingActivityId == null) {
            jdbcTemplate.update(
                    INSERT_DEMO_ACTIVITY_SQL,
                    organizerUserId,
                    organizerUserId,
                    DEMO_TITLE,
                    DEMO_SUMMARY,
                    DEMO_CONTENT,
                    DEMO_LOCATION,
                    startTime,
                    endTime,
                    deadline,
                    DEMO_MAX_PARTICIPANTS,
                    0,
                    ActivityStatus.PUBLISHED.name(),
                    Visibility.PUBLIC.name(),
                    false,
                    now,
                    now,
                    now
            );
            Long insertedActivityId = findDemoActivityId();
            if (insertedActivityId == null) {
                throw new IllegalStateException("Failed to locate inserted online demo activity");
            }
            log.info("Inserted local online demo activity {}", insertedActivityId);
            return insertedActivityId;
        }

        jdbcTemplate.update(
                UPDATE_DEMO_ACTIVITY_SQL,
                organizerUserId,
                organizerUserId,
                DEMO_SUMMARY,
                DEMO_CONTENT,
                startTime,
                endTime,
                deadline,
                DEMO_MAX_PARTICIPANTS,
                ActivityStatus.PUBLISHED.name(),
                Visibility.PUBLIC.name(),
                false,
                now,
                now,
                existingActivityId
        );
        log.info("Updated local online demo activity {}", existingActivityId);
        return existingActivityId;
    }

    private Long findDemoActivityId() {
        try {
            return jdbcTemplate.queryForObject(FIND_DEMO_ACTIVITY_SQL, Long.class, DEMO_TITLE, DEMO_LOCATION);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void ensureApprovedReview(Long activityId) {
        LocalDateTime now = LocalDateTime.now();
        int updated = jdbcTemplate.update(
                UPDATE_DEMO_REVIEW_SQL,
                ReviewStatus.APPROVED.name(),
                DEMO_REVIEW_COMMENT,
                now,
                now,
                activityId
        );
        if (updated > 0) {
            return;
        }

        jdbcTemplate.update(
                INSERT_DEMO_REVIEW_SQL,
                activityId,
                ReviewStatus.APPROVED.name(),
                DEMO_REVIEW_COMMENT,
                now,
                now,
                now
        );
    }
}
