package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityAuditLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_audit_logs.
 */
public interface ActivityAuditLogMapper extends BaseMapper<ActivityAuditLog> {

    /**
     * Read audit history ordered by newest first.
     */
    List<ActivityAuditLog> selectByActivityIdOrderByCreatedAtDesc(@Param("activityId") Long activityId);
}
