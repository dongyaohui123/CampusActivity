package com.campus.activity.service.impl;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.activity.CancelActivityRequest;
import com.campus.activity.dto.activity.CreateActivityRequest;
import com.campus.activity.dto.activity.UpdateActivityRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.service.ActivityService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 活动管理服务实现（旧版接口）。
 */
@Service
public class ActivityServiceImpl implements ActivityService {
    private final ActivityMapper activityMapper;

    public ActivityServiceImpl(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    /**
     * 创建活动并设置默认业务字段。
     *
     * @param request 创建请求参数
     * @return 创建后的活动
     */
    @Override
    @Transactional
    public Activity createActivity(CreateActivityRequest request) {
        validateActivityTime(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        Activity activity = new Activity();
        activity.setOrganizerId(request.getOrganizerId());
        activity.setPublisherId(request.getPublisherId());
        activity.setTitle(request.getTitle());
        activity.setSummary(request.getSummary());
        activity.setContent(request.getContent());
        activity.setCoverUrl(request.getCoverUrl());
        activity.setLocation(request.getLocation());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setRegistrationDeadline(request.getRegistrationDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setRegisteredCount(0);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setVisibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC);
        activity.setFeatured(request.getFeatured() != null ? request.getFeatured() : Boolean.FALSE);

        try {
            activityMapper.insert(activity);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "create activity failed due to relation/constraint conflict");
        }
        return activity;
    }

    /**
     * 更新活动的可修改字段。
     *
     * @param id 活动 ID
     * @param request 更新请求参数
     * @return 更新后的活动
     */
    @Override
    @Transactional
    public Activity updateActivity(Long id, UpdateActivityRequest request) {
        Activity existing = activityMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + id);
        }
        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no updatable field provided");
        }

        LocalDateTime nextStart = request.getStartTime() != null ? request.getStartTime() : existing.getStartTime();
        LocalDateTime nextEnd = request.getEndTime() != null ? request.getEndTime() : existing.getEndTime();
        LocalDateTime nextDeadline = request.getRegistrationDeadline() != null
                ? request.getRegistrationDeadline() : existing.getRegistrationDeadline();
        validateActivityTime(nextStart, nextEnd, nextDeadline);

        if (request.getOrganizerId() != null) {
            existing.setOrganizerId(request.getOrganizerId());
        }
        if (request.getPublisherId() != null) {
            existing.setPublisherId(request.getPublisherId());
        }
        if (request.getTitle() != null) {
            existing.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            existing.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            existing.setContent(request.getContent());
        }
        if (request.getCoverUrl() != null) {
            existing.setCoverUrl(request.getCoverUrl());
        }
        if (request.getLocation() != null) {
            existing.setLocation(request.getLocation());
        }
        if (request.getStartTime() != null) {
            existing.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            existing.setEndTime(request.getEndTime());
        }
        if (request.getRegistrationDeadline() != null) {
            existing.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getMaxParticipants() != null) {
            existing.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getVisibility() != null) {
            existing.setVisibility(request.getVisibility());
        }
        if (request.getFeatured() != null) {
            existing.setFeatured(request.getFeatured());
        }

        try {
            activityMapper.updateById(existing);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "update activity failed due to relation/constraint conflict");
        }
        return existing;
    }

    /**
     * 逻辑取消活动。
     *
     * @param id 活动 ID
     * @param request 取消请求参数
     */
    @Override
    @Transactional
    public void cancelActivity(Long id, CancelActivityRequest request) {
        Activity existing = activityMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + id);
        }
        if (ActivityStatus.CANCELLED.equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity already cancelled");
        }
        existing.setStatus(ActivityStatus.CANCELLED);
        existing.setCancelledAt(LocalDateTime.now());
        existing.setCancelReason(request.getCancelReason());
        activityMapper.updateById(existing);
    }

    /**
     * 按 ID 查询活动。
     *
     * @param id 活动 ID
     * @return 活动详情
     */
    @Override
    public Activity getActivityById(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + id);
        }
        return activity;
    }

    /**
     * 按筛选条件查询活动列表。
     *
     * @param status 活动状态筛选
     * @param visibility 可见性筛选
     * @param organizerId 主办方用户 ID 筛选
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 活动列表
     */
    @Override
    public List<ActivityListItemView> listActivities(
            ActivityStatus status,
            Visibility visibility,
            Long organizerId,
            String keyword,
            LocalDateTime startFrom,
            LocalDateTime startTo
    ) {
        return activityMapper.selectActivityList(status, visibility, organizerId, keyword, startFrom, startTo);
    }

    /**
     * 判断请求中是否至少包含一个可更新字段。
     */
    private boolean hasAnyUpdatableField(UpdateActivityRequest request) {
        return request.getOrganizerId() != null
                || request.getPublisherId() != null
                || request.getTitle() != null
                || request.getSummary() != null
                || request.getContent() != null
                || request.getCoverUrl() != null
                || request.getLocation() != null
                || request.getStartTime() != null
                || request.getEndTime() != null
                || request.getRegistrationDeadline() != null
                || request.getMaxParticipants() != null
                || request.getVisibility() != null
                || request.getFeatured() != null;
    }

    /**
     * 校验活动时间窗口合法性。
     */
    private void validateActivityTime(LocalDateTime start, LocalDateTime end, LocalDateTime registrationDeadline) {
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startTime and endTime are required");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "endTime must be later than startTime");
        }
        if (registrationDeadline != null && registrationDeadline.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "registrationDeadline must be <= startTime");
        }
    }
}
