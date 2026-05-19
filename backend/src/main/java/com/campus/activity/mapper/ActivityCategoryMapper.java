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

    /**
     * 查询启用叶子分类（没有启用子分类）。
     */
    List<ActivityCategory> selectActiveLeafCategories();

    /**
     * 查询全部启用分类。
     */
    List<ActivityCategory> selectActiveCategories();
}
