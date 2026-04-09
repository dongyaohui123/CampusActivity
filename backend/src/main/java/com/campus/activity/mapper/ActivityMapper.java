package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activities.
 */
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * Core list query for management/list page with optional filters.
     */
    List<ActivityListItemView> selectActivityList(
            @Param("status") ActivityStatus status,
            @Param("visibility") Visibility visibility,
            @Param("organizerId") Long organizerId,
            @Param("keyword") String keyword,
            @Param("startFrom") LocalDateTime startFrom,
            @Param("startTo") LocalDateTime startTo
    );
}
