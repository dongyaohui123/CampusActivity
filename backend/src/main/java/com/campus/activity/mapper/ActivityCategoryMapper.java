package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityCategory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_categories.
 */
public interface ActivityCategoryMapper extends BaseMapper<ActivityCategory> {

    /**
     * Query direct children of a parent category.
     */
    List<ActivityCategory> selectByParentId(@Param("parentId") Long parentId);
}
