package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityManagerPermissionGrant;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityManagerPermission;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.BasicStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityManagerPermissionGrantMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1PublicActivityService;
import com.campus.activity.view.v1.ManageableActivityView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 公开活动服务实现（v1）。
 * 仅返回“对外可见且审核通过”的活动数据。
 */
@Service
public class V1PublicActivityServiceImpl implements V1PublicActivityService {
    /**
     * 面向公众展示的活动状态集合。
     */
    private static final Set<ActivityStatus> PUBLIC_STATUS = Set.of(
            ActivityStatus.PUBLISHED,
            ActivityStatus.REGISTRATION_OPEN,
            ActivityStatus.REGISTRATION_CLOSED,
            ActivityStatus.ONGOING,
            ActivityStatus.FINISHED
    );
    private static final Set<ActivityManagerPermission> DEFAULT_MANAGER_PERMISSIONS =
            EnumSet.of(ActivityManagerPermission.VIEW_REGISTRATIONS, ActivityManagerPermission.CHECK_IN);

    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityManagerPermissionGrantMapper managerPermissionGrantMapper;
    private final OperatorPermissionService permissionService;
    private final ActivityPhaseResolver activityPhaseResolver;

    /**
     * 构造函数。
     *
     * @param activityMapper 活动数据访问
     * @param reviewMapper 审核数据访问
     */
    public V1PublicActivityServiceImpl(
            ActivityMapper activityMapper,
            ActivityReviewMapper reviewMapper,
            ActivityManagerPermissionGrantMapper managerPermissionGrantMapper,
            OperatorPermissionService permissionService,
            ActivityPhaseResolver activityPhaseResolver
    ) {
        this.activityMapper = activityMapper;
        this.reviewMapper = reviewMapper;
        this.managerPermissionGrantMapper = managerPermissionGrantMapper;
        this.permissionService = permissionService;
        this.activityPhaseResolver = activityPhaseResolver;
    }

    /**
     * 查询公开活动列表。
     *
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 符合公开条件的活动列表
     */
    @Override
    public List<ActivityListItemView> listPublicActivities(String keyword, LocalDateTime startFrom, LocalDateTime startTo) {
        List<ActivityListItemView> raw = activityMapper.selectActivityList(
                null,
                null,
                null,
                keyword,
                startFrom,
                startTo
        );
        return raw.stream()
                // 同时满足“审核通过 + 公开状态”才对外展示。
                .filter(item -> item.getReviewStatus() == ReviewStatus.APPROVED)
                .filter(item -> item.getStatus() != null && PUBLIC_STATUS.contains(item.getStatus()))
                .map(this::withResolvedStatus)
                .collect(Collectors.toList());
    }

    /**
     * 查询公开活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    @Override
    public Activity getPublicActivityDetail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (!PUBLIC_STATUS.contains(activity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not publicly available");
        }

        // 详情接口与列表保持一致：必须审核通过才允许访问。
        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null || !ReviewStatus.APPROVED.equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not approved yet");
        }
        activity.setStatus(activityPhaseResolver.resolve(activity));
        return activity;
    }

    private ActivityListItemView withResolvedStatus(ActivityListItemView item) {
        item.setStatus(activityPhaseResolver.resolve(
                item.getStatus(),
                item.getStartTime(),
                item.getEndTime(),
                item.getRegistrationDeadline()
        ));
        return item;
    }

    @Override
    public List<ManageableActivityView> listManageableActivities(Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        boolean operatorIsOrganizer = UserRole.ORGANIZER.equals(operator.getRole());

        Map<Long, ActivityPermissionFlags> permissionMap = new HashMap<>();
        Set<Long> activityIds = new HashSet<>();

        if (operatorIsOrganizer) {
            List<Activity> ownActivities = activityMapper.selectList(
                    new QueryWrapper<Activity>().eq("organizer_id", operator.getId())
            );
            for (Activity own : ownActivities) {
                if (own == null || own.getId() == null) {
                    continue;
                }
                activityIds.add(own.getId());
                ActivityPermissionFlags flags = permissionMap.computeIfAbsent(own.getId(), key -> new ActivityPermissionFlags());
                flags.isOrganizer = true;
                flags.canCheckIn = true;
                flags.canViewRegistrations = true;
            }
        }

        List<ActivityManagerPermissionGrant> grants = managerPermissionGrantMapper.selectList(
                new QueryWrapper<ActivityManagerPermissionGrant>()
                        .eq("user_id", operator.getId())
                        .eq("status", BasicStatus.ACTIVE.name())
        );
        for (ActivityManagerPermissionGrant grant : grants) {
            if (grant == null || grant.getActivityId() == null) {
                continue;
            }
            activityIds.add(grant.getActivityId());
            ActivityPermissionFlags flags = permissionMap.computeIfAbsent(grant.getActivityId(), key -> new ActivityPermissionFlags());
            Set<ActivityManagerPermission> permissions = decodeManagerPermissions(grant.getPermissions());
            if (permissions.contains(ActivityManagerPermission.CHECK_IN)) {
                flags.canCheckIn = true;
            }
            if (permissions.contains(ActivityManagerPermission.VIEW_REGISTRATIONS)) {
                flags.canViewRegistrations = true;
            }
        }

        if (activityIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(activityIds).stream()
                .filter(Objects::nonNull)
                .filter(activity -> activity.getId() != null)
                .collect(Collectors.toMap(Activity::getId, activity -> activity));

        List<ManageableActivityView> result = new ArrayList<>();
        for (Long activityId : activityIds) {
            Activity activity = activityMap.get(activityId);
            if (activity == null) {
                continue;
            }
            ActivityPermissionFlags flags = permissionMap.getOrDefault(activityId, new ActivityPermissionFlags());
            ManageableActivityView view = new ManageableActivityView();
            view.setId(activity.getId());
            view.setOrganizerId(activity.getOrganizerId());
            view.setTitle(activity.getTitle());
            view.setCoverUrl(activity.getCoverUrl());
            view.setLocation(activity.getLocation());
            view.setStartTime(activity.getStartTime());
            view.setEndTime(activity.getEndTime());
            view.setRegistrationDeadline(activity.getRegistrationDeadline());
            view.setStatus(activityPhaseResolver.resolve(activity));
            view.setCanCheckIn(Boolean.TRUE.equals(flags.canCheckIn));
            view.setCanViewRegistrations(Boolean.TRUE.equals(flags.canViewRegistrations));
            view.setIsOrganizer(Boolean.TRUE.equals(flags.isOrganizer));
            result.add(view);
        }

        result.sort((left, right) -> {
            int byStart = Comparator.nullsLast(LocalDateTime::compareTo).compare(right.getStartTime(), left.getStartTime());
            if (byStart != 0) {
                return byStart;
            }
            return Comparator.nullsLast(Long::compareTo).compare(right.getId(), left.getId());
        });
        return result;
    }

    private Set<ActivityManagerPermission> decodeManagerPermissions(String raw) {
        if (!StringUtils.hasText(raw)) {
            return EnumSet.copyOf(DEFAULT_MANAGER_PERMISSIONS);
        }
        Set<ActivityManagerPermission> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .map(value -> {
                    try {
                        return ActivityManagerPermission.valueOf(value);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(permission -> permission != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ActivityManagerPermission.class)));
        if (parsed.isEmpty()) {
            return EnumSet.copyOf(DEFAULT_MANAGER_PERMISSIONS);
        }
        return parsed;
    }

    private static class ActivityPermissionFlags {
        private Boolean canViewRegistrations = false;
        private Boolean canCheckIn = false;
        private Boolean isOrganizer = false;
    }
}
