package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityCheckinRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityCategory;
import com.campus.activity.entity.ActivityCategoryRel;
import com.campus.activity.entity.ActivityAuditLog;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.LocationCampusMapping;
import com.campus.activity.entity.User;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.AuditAction;
import com.campus.activity.enums.BasicStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityCategoryMapper;
import com.campus.activity.mapper.ActivityCategoryRelMapper;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.mapper.LocationCampusMappingMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import com.campus.activity.view.v1.CheckinResultView;
import com.campus.activity.view.v1.OrganizerActivityOptionsView;
import com.campus.activity.view.v1.OrganizerActivityTypeOptionView;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 组织者活动服务实现（v1）。
 * 提供组织者创建、修改、提审、列表查询以及报名用户查询能力。
 */
@Service
public class V1OrganizerActivityServiceImpl implements V1OrganizerActivityService {
    private static final String DEFAULT_CATEGORY_NAME = "未分类";
    private static final String CAMPUS_SOUTH = "SOUTH";
    private static final String CAMPUS_NORTH = "NORTH";
    private static final String CAMPUS_ONLINE = "ONLINE";
    private static final List<String> DEFAULT_CAMPUS_TYPES = List.of(CAMPUS_SOUTH, CAMPUS_NORTH, CAMPUS_ONLINE);
    private static final Set<String> SUPPORTED_CAMPUS_TYPES = Set.of(CAMPUS_SOUTH, CAMPUS_NORTH, CAMPUS_ONLINE);

    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper activityCategoryMapper;
    private final ActivityCategoryRelMapper activityCategoryRelMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityAuditLogMapper auditLogMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final LocationCampusMappingMapper locationCampusMappingMapper;
    private final UserMapper userMapper;
    private final OperatorPermissionService permissionService;

    /**
     * 构造函数。
     */
    public V1OrganizerActivityServiceImpl(
            ActivityMapper activityMapper,
            ActivityCategoryMapper activityCategoryMapper,
            ActivityCategoryRelMapper activityCategoryRelMapper,
            ActivityReviewMapper reviewMapper,
            ActivityAuditLogMapper auditLogMapper,
            ActivityRegistrationMapper registrationMapper,
            LocationCampusMappingMapper locationCampusMappingMapper,
            UserMapper userMapper,
            OperatorPermissionService permissionService
    ) {
        this.activityMapper = activityMapper;
        this.activityCategoryMapper = activityCategoryMapper;
        this.activityCategoryRelMapper = activityCategoryRelMapper;
        this.reviewMapper = reviewMapper;
        this.auditLogMapper = auditLogMapper;
        this.registrationMapper = registrationMapper;
        this.locationCampusMappingMapper = locationCampusMappingMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    /**
     * 组织者创建活动。
     *
     * @param request 创建请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity createActivity(OrganizerActivityCreateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        validateActivityTime(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());
        String campusCode = normalizeCampusCode(request.getCampusCode(), true);
        String validatedLocation = normalizeLocationByCampus(request.getLocation(), campusCode);
        ActivityCategory validatedCategory = requireActiveCategory(request.getActivityTypeId());

        Activity activity = new Activity();
        activity.setOrganizerId(operator.getId());
        activity.setPublisherId(operator.getId());
        activity.setTitle(request.getTitle());
        activity.setSummary(request.getSummary());
        activity.setContent(request.getContent());
        activity.setCoverUrl(request.getCoverUrl());
        activity.setLocation(validatedLocation);
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setRegistrationDeadline(request.getRegistrationDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setRegisteredCount(0);
        activity.setFeatured(Boolean.FALSE);
        activity.setStatus(ActivityStatus.DRAFT);
        activityMapper.insert(activity);
        replaceActivityCategoryRelation(activity.getId(), validatedCategory.getId());
        upsertLocationMapping(validatedLocation, campusCode);
        return activity;
    }

    /**
     * 组织者更新活动。
     *
     * @param activityId 活动 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity updateActivity(Long activityId, OrganizerActivityUpdateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);

        Activity activity = getOwnedActivityOrThrow(activityId, operator.getId());
        if (ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled activity cannot be updated");
        }
        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no updatable field provided");
        }

        // 更新时间前先合并新旧时间，统一做时间窗口校验。
        LocalDateTime mergedStart = request.getStartTime() != null ? request.getStartTime() : activity.getStartTime();
        LocalDateTime mergedEnd = request.getEndTime() != null ? request.getEndTime() : activity.getEndTime();
        LocalDateTime mergedDeadline = request.getRegistrationDeadline() != null
                ? request.getRegistrationDeadline() : activity.getRegistrationDeadline();
        validateActivityTime(mergedStart, mergedEnd, mergedDeadline);
        ActivityCategory validatedCategory = null;
        if (request.getActivityTypeId() != null) {
            validatedCategory = requireActiveCategory(request.getActivityTypeId());
        }
        String effectiveCampusCode = request.getCampusCode() != null
                ? normalizeCampusCode(request.getCampusCode(), true)
                : resolveCampusCodeByLocation(activity.getLocation());

        if (request.getTitle() != null) {
            activity.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            activity.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            activity.setContent(request.getContent());
        }
        if (request.getCoverUrl() != null) {
            activity.setCoverUrl(request.getCoverUrl());
        }
        if (request.getLocation() != null || request.getCampusCode() != null) {
            String locationSource = request.getLocation() != null ? request.getLocation() : activity.getLocation();
            activity.setLocation(normalizeLocationByCampus(locationSource, effectiveCampusCode));
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getRegistrationDeadline() != null) {
            activity.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getMaxParticipants() != null) {
            activity.setMaxParticipants(request.getMaxParticipants());
        }
        activityMapper.updateById(activity);
        if (validatedCategory != null) {
            replaceActivityCategoryRelation(activity.getId(), validatedCategory.getId());
        }
        if (request.getLocation() != null || request.getCampusCode() != null) {
            upsertLocationMapping(activity.getLocation(), effectiveCampusCode);
        }
        return activity;
    }

    /**
     * 组织者提交审核。
     *
     * @param activityId 活动 ID
     * @param request 提交审核请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity submitForReview(Long activityId, SubmitReviewRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        Activity activity = getOwnedActivityOrThrow(activityId, operator.getId());
        if (ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled activity cannot be submitted");
        }

        // 提审后活动进入可发布状态，同时写入/重置审核记录。
        activity.setStatus(ActivityStatus.PUBLISHED);
        if (activity.getPublishedAt() == null) {
            activity.setPublishedAt(LocalDateTime.now());
        }
        activityMapper.updateById(activity);

        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null) {
            review = new ActivityReview();
            review.setActivityId(activityId);
            review.setReviewStatus(ReviewStatus.PENDING);
            reviewMapper.insert(review);
        } else {
            review.setReviewStatus(ReviewStatus.PENDING);
            review.setReviewerId(null);
            review.setReviewComment(null);
            review.setReviewedAt(null);
            reviewMapper.updateById(review);
        }

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityId(activityId);
        log.setOperatorId(operator.getId());
        log.setAction(AuditAction.SUBMIT);
        log.setComment(request.getComment());
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
        return activity;
    }

    /**
     * 查询组织者发布活动可选项。
     */
    @Override
    public OrganizerActivityOptionsView getActivityOptions(Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);

        List<String> campusTypes = buildCampusTypeOptions(locationCampusMappingMapper.selectEnabledCampusCodes());
        List<ActivityCategory> categories = activityCategoryMapper.selectActiveLeafCategories();
        if (categories == null || categories.isEmpty()) {
            categories = activityCategoryMapper.selectActiveCategories();
        }

        List<OrganizerActivityTypeOptionView> activityTypes = categories == null
                ? Collections.emptyList()
                : categories.stream().map(this::toTypeOption).toList();

        OrganizerActivityOptionsView optionsView = new OrganizerActivityOptionsView();
        optionsView.setCampusTypes(campusTypes);
        optionsView.setActivityTypes(activityTypes);
        return optionsView;
    }

    /**
     * 查询组织者活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @param keyword 关键字筛选
     * @param startFrom 开始时间下界
     * @param startTo 开始时间上界
     * @return 活动列表
     */
    @Override
    public List<ActivityListItemView> listOwnActivities(
            Long operatorUserId,
            UserRole operatorRole,
            String keyword,
            LocalDateTime startFrom,
            LocalDateTime startTo
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        return activityMapper.selectActivityList(null, null, operator.getId(), keyword, startFrom, startTo);
    }

    /**
     * 查询活动报名用户列表。
     *
     * @param activityId 活动 ID
     * @param status 报名状态筛选
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名用户列表
     */
    @Override
    public List<ActivityRegistrationUserView> listActivityRegistrations(
            Long activityId,
            RegistrationStatus status,
            Long operatorUserId,
            UserRole operatorRole
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        getOwnedActivityOrThrow(activityId, operator.getId());

        QueryWrapper<ActivityRegistration> wrapper = new QueryWrapper<ActivityRegistration>()
                .eq("activity_id", activityId)
                .orderByDesc("registered_at", "id");
        if (status != null) {
            wrapper.eq("status", status.name());
        }
        List<ActivityRegistration> registrations = registrationMapper.selectList(wrapper);
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = registrations.stream()
                .map(ActivityRegistration::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<ActivityRegistrationUserView> result = new ArrayList<>(registrations.size());
        for (ActivityRegistration registration : registrations) {
            ActivityRegistrationUserView view = new ActivityRegistrationUserView();
            view.setRegistrationId(registration.getId());
            view.setUserId(registration.getUserId());
            view.setStatus(registration.getStatus());
            view.setRemark(registration.getRemark());
            view.setRegisteredAt(registration.getRegisteredAt());
            view.setCancelledAt(registration.getCancelledAt());
            view.setCheckinAt(registration.getCheckinAt());
            User user = userMap.get(registration.getUserId());
            if (user != null) {
                view.setNickname(user.getNickname());
                view.setPhone(user.getPhone());
            }
            result.add(view);
        }
        return result;
    }

    /**
     * 组织者按票码签到。
     */
    @Override
    @Transactional
    public CheckinResultView checkInByTicketCode(
            Long activityId,
            OrganizerActivityCheckinRequest request,
            Long operatorUserId,
            UserRole operatorRole
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        getOwnedActivityOrThrow(activityId, operator.getId());

        ActivityRegistration registration = registrationMapper.selectOne(
                new QueryWrapper<ActivityRegistration>()
                        .eq("ticket_code", request.getTicketCode())
                        .last("LIMIT 1")
        );
        if (registration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ticket not found");
        }
        if (!activityId.equals(registration.getActivityId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "ticket does not belong to this activity");
        }
        if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled ticket cannot check in");
        }
        if (RegistrationStatus.CHECKED_IN.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "ticket already checked in");
        }
        if (!RegistrationStatus.REGISTERED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration status cannot check in");
        }

        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registration.setCheckinAt(LocalDateTime.now());
        registration.setCheckinOperatorId(operator.getId());
        registrationMapper.updateById(registration);

        User attendee = userMapper.selectById(registration.getUserId());
        CheckinResultView result = new CheckinResultView();
        result.setRegistrationId(registration.getId());
        result.setActivityId(registration.getActivityId());
        result.setUserId(registration.getUserId());
        result.setNickname(attendee != null ? attendee.getNickname() : null);
        result.setStatus(registration.getStatus());
        result.setCheckedInAt(registration.getCheckinAt());
        result.setOperatorUserId(operator.getId());
        return result;
    }

    /**
     * 获取组织者名下活动。
     * 统一校验活动存在且归属当前组织者。
     */
    private Activity getOwnedActivityOrThrow(Long activityId, Long organizerId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (!organizerId.equals(activity.getOrganizerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "organizer cannot operate another organizer's activity");
        }
        return activity;
    }

    /**
     * 判断更新请求中是否至少包含一个可更新字段。
     */
    private boolean hasAnyUpdatableField(OrganizerActivityUpdateRequest request) {
        return StringUtils.hasText(request.getTitle())
                || StringUtils.hasText(request.getSummary())
                || request.getContent() != null
                || request.getCoverUrl() != null
                || StringUtils.hasText(request.getLocation())
                || StringUtils.hasText(request.getCampusCode())
                || request.getActivityTypeId() != null
                || request.getStartTime() != null
                || request.getEndTime() != null
                || request.getRegistrationDeadline() != null
                || request.getMaxParticipants() != null;
    }

    private List<String> buildCampusTypeOptions(List<String> existingCampusCodes) {
        if (existingCampusCodes == null || existingCampusCodes.isEmpty()) {
            return new ArrayList<>(DEFAULT_CAMPUS_TYPES);
        }
        List<String> normalized = existingCampusCodes.stream()
                .map(code -> normalizeCampusCode(code, false))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return new ArrayList<>(DEFAULT_CAMPUS_TYPES);
        }
        return new ArrayList<>(DEFAULT_CAMPUS_TYPES);
    }

    private String normalizeCampusCode(String campusCode, boolean required) {
        String normalized = String.valueOf(campusCode == null ? "" : campusCode).trim().toUpperCase();
        if (!StringUtils.hasText(normalized)) {
            if (required) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "campusCode is required");
            }
            return "";
        }
        if (!SUPPORTED_CAMPUS_TYPES.contains(normalized)) {
            if (!required) {
                return "";
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "campusCode is invalid: " + campusCode);
        }
        return normalized;
    }

    private String normalizeLocationByCampus(String location, String campusCode) {
        String normalized = location == null ? "" : location.trim();
        if (CAMPUS_ONLINE.equals(campusCode)) {
            return StringUtils.hasText(normalized) ? normalized : "线上";
        }
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "location is required for SOUTH/NORTH campus");
        }
        return normalized;
    }

    private String resolveCampusCodeByLocation(String location) {
        String normalized = location == null ? "" : location.trim();
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        LocationCampusMapping mapping = locationCampusMappingMapper.selectEnabledByLocationName(normalized);
        if (mapping != null) {
            String mappedCode = normalizeCampusCode(mapping.getCampusCode(), false);
            if (StringUtils.hasText(mappedCode)) {
                return mappedCode;
            }
        }
        if (normalized.contains("线上")) {
            return CAMPUS_ONLINE;
        }
        return CAMPUS_NORTH;
    }

    private void upsertLocationMapping(String location, String campusCode) {
        if (!StringUtils.hasText(location) || !StringUtils.hasText(campusCode)) {
            return;
        }
        locationCampusMappingMapper.upsertMapping(location.trim(), campusCode);
    }

    /**
     * 校验活动类型必须存在且启用。
     */
    private ActivityCategory requireActiveCategory(Long activityTypeId) {
        if (activityTypeId == null || activityTypeId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activityTypeId is required");
        }
        ActivityCategory category = activityCategoryMapper.selectById(activityTypeId);
        if (category == null || category.getStatus() != BasicStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activityTypeId is invalid: " + activityTypeId);
        }
        return category;
    }

    /**
     * 单选语义：先清空旧关系，再写入新关系。
     */
    private void replaceActivityCategoryRelation(Long activityId, Long categoryId) {
        activityCategoryRelMapper.deleteByActivityId(activityId);
        ActivityCategoryRel relation = new ActivityCategoryRel();
        relation.setActivityId(activityId);
        relation.setCategoryId(categoryId);
        relation.setCreatedAt(LocalDateTime.now());
        activityCategoryRelMapper.insertRelation(relation);
    }

    private OrganizerActivityTypeOptionView toTypeOption(ActivityCategory category) {
        OrganizerActivityTypeOptionView option = new OrganizerActivityTypeOptionView();
        option.setId(category.getId());
        option.setName(StringUtils.hasText(category.getName()) ? category.getName() : DEFAULT_CATEGORY_NAME);
        return option;
    }

    /**
     * 校验活动时间窗口。
     * 规则：start/end 必填、end 必须晚于 start、报名截止时间不得晚于 start。
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
