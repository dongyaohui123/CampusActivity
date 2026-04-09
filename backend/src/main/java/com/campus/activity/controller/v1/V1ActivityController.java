package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.service.v1.V1PublicActivityService;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/activities")
public class V1ActivityController {
    private final V1PublicActivityService publicActivityService;

    public V1ActivityController(V1PublicActivityService publicActivityService) {
        this.publicActivityService = publicActivityService;
    }

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

    @GetMapping("/{activityId}")
    public ApiResponse<Activity> getPublicActivityDetail(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId
    ) {
        return ApiResponse.success(publicActivityService.getPublicActivityDetail(activityId));
    }
}
