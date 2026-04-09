package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.OrganizerFollow;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for organizer_follows.
 */
public interface OrganizerFollowMapper extends BaseMapper<OrganizerFollow> {

    /**
     * Check whether user follows organizer.
     */
    Boolean existsFollow(@Param("organizerId") Long organizerId,
                         @Param("userId") Long userId);
}
