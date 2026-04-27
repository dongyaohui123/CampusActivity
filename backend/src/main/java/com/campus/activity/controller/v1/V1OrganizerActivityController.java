package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织者活动管理控制器（v1）。
 * 覆盖活动创建、编辑、提审、列表查询、报名用户查询。
 */
@Validated
@RestController
@RequestMapping("/api/v1/organizer/activities")
public class V1OrganizerActivityController {
    private final V1OrganizerActivityService organizerActivityService;

    public V1OrganizerActivityController(V1OrganizerActivityService organizerActivityService) {
        this.organizerActivityService = organizerActivityService;
    }

    /**
     * 组织者创建活动。
     *
     * @param request 创建参数
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @return 创建后的活动
     */
    @PostMapping
    public ApiResponse<Activity> createActivity(
            @Valid @RequestBody OrganizerActivityCreateRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("activity created",
                organizerActivityService.createActivity(request, operatorUserId, operatorRole));
    }

    /**
     * 组织者更新活动。
     *
     * @param activityId 活动 ID
     * @param request 更新参数
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @return 更新后的活动
     */
    @PutMapping("/{activityId}")
    public ApiResponse<Activity> updateActivity(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody OrganizerActivityUpdateRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("activity updated",
                organizerActivityService.updateActivity(activityId, request, operatorUserId, operatorRole));
    }

    /**
     * 提交活动审核。
     *
     * @param activityId 活动 ID
     * @param request 提审说明（可空）
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @return 提交后的活动
     */
    @PostMapping("/{activityId}/submit-review")
    public ApiResponse<Activity> submitForReview(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody(required = false) SubmitReviewRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        SubmitReviewRequest safeRequest = request == null ? new SubmitReviewRequest() : request;
        return ApiResponse.success("review submitted",
                organizerActivityService.submitForReview(activityId, safeRequest, operatorUserId, operatorRole));
    }

    /**
     * 查询当前组织者名下活动列表。
     *
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 组织者活动列表
     */
    @GetMapping
    public ApiResponse<List<ActivityListItemView>> listOwnActivities(
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(value = "startTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo
    ) {
        return ApiResponse.success(organizerActivityService.listOwnActivities(
                operatorUserId, operatorRole, keyword, startFrom, startTo
        ));
    }

    /**
     * 查询活动报名用户列表。
     *
     * @param activityId 活动 ID
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @param status 报名状态筛选（可空）
     * @return 报名用户列表
     */
    @GetMapping("/{activityId}/registrations")
    public ApiResponse<List<ActivityRegistrationUserView>> listActivityRegistrations(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole,
            @RequestParam(value = "status", required = false) RegistrationStatus status
    ) {
        return ApiResponse.success(organizerActivityService.listActivityRegistrations(
                activityId, status, operatorUserId, operatorRole
        ));
    }
}
