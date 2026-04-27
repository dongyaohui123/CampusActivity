SET @demo_title := '线上经验分享会';
SET @demo_location := '线上';
SET @demo_summary := '远程参与的线上经验分享活动，报名后按说明加入。';
SET @demo_content := '本场活动在线上进行，报名成功后可根据活动说明按时参加。';
SET @demo_max_participants := 200;
SET @demo_start_time := DATE_ADD(NOW(), INTERVAL 7 DAY);
SET @demo_end_time := DATE_ADD(@demo_start_time, INTERVAL 2 HOUR);
SET @demo_deadline := DATE_SUB(@demo_start_time, INTERVAL 1 DAY);

UPDATE activities
SET location = '大学生活动中心'
WHERE location = '图书馆报告厅';

UPDATE activities
SET location = '操场'
WHERE location = '大礼堂';

SET @organizer_user_id := (
    SELECT u.id
    FROM users u
    INNER JOIN organizer_profiles op ON op.user_id = u.id
    WHERE u.role = 'ORGANIZER'
      AND u.status = 'ACTIVE'
    ORDER BY u.id
    LIMIT 1
);

UPDATE activities
SET organizer_id = COALESCE(organizer_id, @organizer_user_id),
    publisher_id = COALESCE(publisher_id, @organizer_user_id),
    summary = @demo_summary,
    content = @demo_content,
    start_time = @demo_start_time,
    end_time = @demo_end_time,
    registration_deadline = @demo_deadline,
    max_participants = @demo_max_participants,
    registered_count = COALESCE(registered_count, 0),
    status = 'PUBLISHED',
    visibility = 'PUBLIC',
    is_featured = 0,
    published_at = COALESCE(published_at, NOW()),
    updated_at = NOW()
WHERE title = @demo_title
  AND location = @demo_location;

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
)
SELECT
    @organizer_user_id,
    @organizer_user_id,
    @demo_title,
    @demo_summary,
    @demo_content,
    @demo_location,
    @demo_start_time,
    @demo_end_time,
    @demo_deadline,
    @demo_max_participants,
    0,
    'PUBLISHED',
    'PUBLIC',
    0,
    NOW(),
    NOW(),
    NOW()
FROM DUAL
WHERE @organizer_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM activities
      WHERE title = @demo_title
        AND location = @demo_location
  );

SET @demo_activity_id := (
    SELECT a.id
    FROM activities a
    WHERE a.title = @demo_title
      AND a.location = @demo_location
    ORDER BY a.id
    LIMIT 1
);

UPDATE activity_reviews
SET review_status = 'APPROVED',
    review_comment = '初始化线上演示活动',
    reviewed_at = NOW(),
    updated_at = NOW()
WHERE activity_id = @demo_activity_id;

INSERT INTO activity_reviews (
    activity_id,
    review_status,
    reviewer_id,
    review_comment,
    reviewed_at,
    created_at,
    updated_at
)
SELECT
    @demo_activity_id,
    'APPROVED',
    NULL,
    '初始化线上演示活动',
    NOW(),
    NOW(),
    NOW()
FROM DUAL
WHERE @demo_activity_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM activity_reviews
      WHERE activity_id = @demo_activity_id
  );
