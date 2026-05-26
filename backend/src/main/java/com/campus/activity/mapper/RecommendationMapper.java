package com.campus.activity.mapper;

import com.campus.activity.view.v1.RecommendCandidateView;
import com.campus.activity.view.v1.CategoryAffinityRow;
import com.campus.activity.view.v1.OrganizerAffinityRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 推荐系统数据访问接口。
 */
@Mapper
public interface RecommendationMapper {

    /**
     * 查询可推荐的活动候选集。
     * 条件：审核通过、公开可见、状态为 PUBLISHED/REGISTRATION_OPEN/REGISTRATION_CLOSED/ONGOING。
     *
     * @return 候选活动列表
     */
    List<RecommendCandidateView> selectRecommendationCandidates();

    /**
     * 查询用户的分类亲和度。
     * 基于用户报名、收藏、评论的活动分类统计。
     *
     * @param userId 用户 ID
     * @return 分类亲和度列表（categoryId, score）
     */
    List<CategoryAffinityRow> selectUserCategoryAffinity(@Param("userId") Long userId);

    /**
     * 查询用户的组织者亲和度。
     * 基于用户报名、收藏、关注的组织者统计。
     *
     * @param userId 用户 ID
     * @return 组织者亲和度列表（organizerId, score）
     */
    List<OrganizerAffinityRow> selectUserOrganizerAffinity(@Param("userId") Long userId);

    /**
     * 查询同专业用户的分类偏好（冷启动用）。
     * 基于同学院用户的报名活动分类统计。
     *
     * @param college 学院
     * @param major 专业
     * @param excludeUserId 排除的用户 ID（当前用户）
     * @return 分类亲和度列表
     */
    List<CategoryAffinityRow> selectPeerCategoryAffinity(
            @Param("college") String college,
            @Param("major") String major,
            @Param("excludeUserId") Long excludeUserId
    );
}
