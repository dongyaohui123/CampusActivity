package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1ActivityFavoriteService;
import com.campus.activity.service.v1.V1PublicActivityService;
import com.campus.activity.view.v1.ActivityFavoriteStateView;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共活动查询控制器（v1）。
 * 仅提供面向游客/普通用户的公开活动列表与详情查询接口。
 */
@Validated
@RestController
@RequestMapping("/api/v1/activities")
public class V1ActivityController {
    private final V1PublicActivityService publicActivityService;
    private final V1ActivityFavoriteService activityFavoriteService;

    /**
     * 构造函数。
     *
     * @param publicActivityService 公开活动服务
     */
    public V1ActivityController(
            V1PublicActivityService publicActivityService,
            V1ActivityFavoriteService activityFavoriteService
    ) {
        this.publicActivityService = publicActivityService;
        this.activityFavoriteService = activityFavoriteService;
    }

    /**
     * 查询公开活动列表。
     *
     * @param keyword 标题/摘要关键字（可选）
     * @param startFrom 活动开始时间下界（可选）
     * @param startTo 活动开始时间上界（可选）
     * @return 公开活动列表
     */
    @GetMapping
    public ApiResponse<List<ActivityListItemView>> listPublicActivities(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(value = "startTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo
    ) {
        return ApiResponse.success(publicActivityService.listPublicActivities(keyword, startFrom, startTo));
    }

    /**
     * 查询公开活动详情。
     *
     * @param activityId 活动 ID（必须 >= 1）
     * @return 活动详情
     */
    @GetMapping("/{activityId}")
    public ApiResponse<Activity> getPublicActivityDetail(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId
    ) {
        return ApiResponse.success(publicActivityService.getPublicActivityDetail(activityId));
    }

    /**
     * 查询活动收藏状态。
     */
    @GetMapping("/{activityId}/favorite")
    public ApiResponse<ActivityFavoriteStateView> getFavoriteState(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(
                "favorite state",
                activityFavoriteService.getFavoriteState(activityId, operatorUserId, operatorRole)
        );
    }

    /**
     * 收藏活动。
     */
    @PostMapping("/{activityId}/favorite")
    public ApiResponse<ActivityFavoriteStateView> favoriteActivity(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(
                "activity favorited",
                activityFavoriteService.favoriteActivity(activityId, operatorUserId, operatorRole)
        );
    }

    /**
     * 取消收藏活动。
     */
    @DeleteMapping("/{activityId}/favorite")
    public ApiResponse<ActivityFavoriteStateView> unfavoriteActivity(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(
                "activity unfavorited",
                activityFavoriteService.unfavoriteActivity(activityId, operatorUserId, operatorRole)
        );
    }
}
