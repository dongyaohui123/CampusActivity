package com.campus.activity.controller;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.activity.CancelActivityRequest;
import com.campus.activity.dto.activity.CreateActivityRequest;
import com.campus.activity.dto.activity.UpdateActivityRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import com.campus.activity.service.ActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动管理控制器（旧版接口）。
 */
@Validated
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 创建活动。
     *
     * @param request 创建请求参数
     * @return 创建后的活动信息
     */
    @PostMapping
    public ApiResponse<Activity> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.success("activity created", activityService.createActivity(request));
    }

    /**
     * 更新活动。
     *
     * @param id 活动 ID
     * @param request 更新请求参数
     * @return 更新后的活动信息
     */
    @PutMapping("/{id}")
    public ApiResponse<Activity> updateActivity(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id,
            @Valid @RequestBody UpdateActivityRequest request
    ) {
        return ApiResponse.success("activity updated", activityService.updateActivity(id, request));
    }

    /**
     * 取消活动。
     *
     * @param id 活动 ID
     * @param request 取消请求参数
     * @return 是否处理成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> cancelActivity(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id,
            @Valid @RequestBody CancelActivityRequest request
    ) {
        activityService.cancelActivity(id, request);
        return ApiResponse.success("activity cancelled", Boolean.TRUE);
    }

    /**
     * 按 ID 查询活动详情。
     *
     * @param id 活动 ID
     * @return 活动详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Activity> getActivityById(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id
    ) {
        return ApiResponse.success(activityService.getActivityById(id));
    }

    /**
     * 按条件分页前置查询活动列表（不含分页参数）。
     *
     * @param status 活动状态筛选
     * @param visibility 可见性筛选
     * @param organizerId 主办方用户 ID 筛选
     * @param keyword 标题/摘要关键字
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 活动列表
     */
    @GetMapping("/list")
    public ApiResponse<List<ActivityListItemView>> listActivities(
            @RequestParam(value = "status", required = false) ActivityStatus status,
            @RequestParam(value = "visibility", required = false) Visibility visibility,
            @RequestParam(value = "organizerId", required = false) Long organizerId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(value = "startTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo
    ) {
        return ApiResponse.success(activityService.listActivities(
                status, visibility, organizerId, keyword, startFrom, startTo
        ));
    }
}
