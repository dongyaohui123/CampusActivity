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

@Validated
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ApiResponse<Activity> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.success("activity created", activityService.createActivity(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Activity> updateActivity(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id,
            @Valid @RequestBody UpdateActivityRequest request
    ) {
        return ApiResponse.success("activity updated", activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> cancelActivity(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id,
            @Valid @RequestBody CancelActivityRequest request
    ) {
        activityService.cancelActivity(id, request);
        return ApiResponse.success("activity cancelled", Boolean.TRUE);
    }

    @GetMapping("/{id}")
    public ApiResponse<Activity> getActivityById(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id
    ) {
        return ApiResponse.success(activityService.getActivityById(id));
    }

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
