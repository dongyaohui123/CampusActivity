package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityCheckinRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.dto.v1.activity.AddActivityManagerRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1OrganizerActivityCoverService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.view.v1.ActivityManagerView;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import com.campus.activity.view.v1.ActivityCoverUploadView;
import com.campus.activity.view.v1.CheckinResultView;
import com.campus.activity.view.v1.OrganizerActivityOptionsView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;

/**
 * 组织者活动管理控制器（v1）。
 * 覆盖活动创建、编辑、提审、列表查询、报名用户查询。
 */
@Validated
@RestController
@RequestMapping("/api/v1/organizer/activities")
public class V1OrganizerActivityController {
    private final V1OrganizerActivityService organizerActivityService;
    private final V1OrganizerActivityCoverService coverService;

    public V1OrganizerActivityController(
            V1OrganizerActivityService organizerActivityService,
            V1OrganizerActivityCoverService coverService
    ) {
        this.organizerActivityService = organizerActivityService;
        this.coverService = coverService;
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
     * 组织者上传活动封面图。
     */
    @RequestMapping(
            value = "/cover",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ActivityCoverUploadView> uploadCover(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("activity cover uploaded", coverService.uploadCover(file, operatorUserId, operatorRole));
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
     * 查询发布活动可选项（地点、活动类型）。
     */
    @GetMapping("/options")
    public ApiResponse<OrganizerActivityOptionsView> getActivityOptions(
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(organizerActivityService.getActivityOptions(operatorUserId, operatorRole));
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

    /**
     * 组织者按票码签到。
     */
    @PostMapping("/{activityId}/check-in")
    public ApiResponse<CheckinResultView> checkInByTicketCode(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody OrganizerActivityCheckinRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("check-in success",
                organizerActivityService.checkInByTicketCode(activityId, request, operatorUserId, operatorRole));
    }

    /**
     * 查询活动管理员列表。
     */
    @GetMapping("/{activityId}/managers")
    public ApiResponse<List<ActivityManagerView>> listActivityManagers(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(organizerActivityService.listActivityManagers(activityId, operatorUserId, operatorRole));
    }

    /**
     * 添加活动管理员。
     */
    @PostMapping("/{activityId}/managers")
    public ApiResponse<ActivityManagerView> addActivityManager(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody AddActivityManagerRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("activity manager added",
                organizerActivityService.addActivityManager(activityId, request, operatorUserId, operatorRole));
    }

    /**
     * 移除活动管理员。
     */
    @DeleteMapping("/{activityId}/managers/{managerUserId}")
    public ApiResponse<Void> removeActivityManager(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @PathVariable("managerUserId") @Min(value = 1, message = "managerUserId must be >= 1") Long managerUserId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        organizerActivityService.removeActivityManager(activityId, managerUserId, operatorUserId, operatorRole);
        return ApiResponse.success("activity manager removed", null);
    }
}
