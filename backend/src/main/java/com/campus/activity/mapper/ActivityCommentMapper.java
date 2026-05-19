package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityComment;
import com.campus.activity.view.v1.ActivityCommentView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_comments.
 */
public interface ActivityCommentMapper extends BaseMapper<ActivityComment> {

    /**
     * Query comments for a public activity.
     */
    List<ActivityCommentView> selectActivityComments(@Param("activityId") Long activityId);
}
