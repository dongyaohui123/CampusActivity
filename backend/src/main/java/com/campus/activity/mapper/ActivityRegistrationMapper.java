package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.view.ActivityRegistrationCountView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_registrations.
 */
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

    /**
     * Count effective registrations for one activity.
     */
    Long countEffectiveByActivityId(@Param("activityId") Long activityId);

    /**
     * Batch count effective registrations grouped by activity.
     */
    List<ActivityRegistrationCountView> countEffectiveByActivityIds(@Param("activityIds") List<Long> activityIds);

    /**
     * Check whether a user currently has an active registration.
     */
    Boolean existsEffectiveRegistration(@Param("activityId") Long activityId,
                                        @Param("userId") Long userId);
}
