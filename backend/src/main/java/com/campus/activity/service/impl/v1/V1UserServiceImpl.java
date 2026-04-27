package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.user.UserPasswordChangeRequest;
import com.campus.activity.dto.v1.user.UserProfileUpdateRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.AvatarStorageService;
import com.campus.activity.service.v1.AvatarUrlService;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1UserService;
import com.campus.activity.view.v1.AvatarUploadView;
import com.campus.activity.view.v1.RegistrationRecordView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务实现（v1）。
 * 负责用户资料查询/更新与用户报名记录聚合。
 */
@Service
public class V1UserServiceImpl implements V1UserService {
    private final UserMapper userMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final OperatorPermissionService permissionService;
    private final AvatarStorageService avatarStorageService;
    private final AvatarUrlService avatarUrlService;
    private final ActivityPhaseResolver activityPhaseResolver;

    /**
     * 构造函数。
     */
    public V1UserServiceImpl(
            UserMapper userMapper,
            ActivityRegistrationMapper registrationMapper,
            ActivityMapper activityMapper,
            OperatorPermissionService permissionService,
            AvatarStorageService avatarStorageService,
            AvatarUrlService avatarUrlService,
            ActivityPhaseResolver activityPhaseResolver
    ) {
        this.userMapper = userMapper;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.permissionService = permissionService;
        this.avatarStorageService = avatarStorageService;
        this.avatarUrlService = avatarUrlService;
        this.activityPhaseResolver = activityPhaseResolver;
    }

    /**
     * 查询用户资料。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 用户资料
     */
    @Override
    public User getUserProfile(Long userId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }
        // 返回前移除敏感字段。
        user.setPasswordHash(null);
        user.setAvatarUrl(avatarUrlService.toPublicUrl(user.getAvatarUrl()));
        return user;
    }

    /**
     * 更新用户资料。
     *
     * @param userId 目标用户 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的用户资料
     */
    @Override
    @Transactional
    public User updateUserProfile(Long userId, UserProfileUpdateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }
        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no profile field provided");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(avatarUrlService.normalizeForStorage(request.getAvatarUrl()));
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userMapper.updateById(user);
        user.setPasswordHash(null);
        user.setAvatarUrl(avatarUrlService.toPublicUrl(user.getAvatarUrl()));
        return user;
    }

    @Override
    public AvatarUploadView uploadAvatar(Long userId, MultipartFile file, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }

        String avatarUrl = avatarStorageService.storeAvatar(userId, file);
        AvatarUploadView view = new AvatarUploadView();
        view.setAvatarUrl(avatarUrlService.toPublicUrl(avatarUrl));
        return view;
    }

    /**
     * 修改用户密码。
     *
     * @param userId 目标用户 ID
     * @param request 修改密码请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     */
    @Override
    @Transactional
    public void changeUserPassword(Long userId, UserPasswordChangeRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelf(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }
        if (!Objects.equals(user.getPasswordHash(), request.getOldPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "old password is incorrect");
        }
        if (Objects.equals(request.getOldPassword(), request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "new password must be different from old password");
        }

        user.setPasswordHash(request.getNewPassword());
        user.setForcePasswordChange(Boolean.FALSE);
        userMapper.updateById(user);
    }

    /**
     * 查询用户报名记录。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录列表
     */
    @Override
    public List<RegistrationRecordView> getUserRegistrations(Long userId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        List<ActivityRegistration> registrations = registrationMapper.selectList(
                new QueryWrapper<ActivityRegistration>()
                        .eq("user_id", userId)
                        .orderByDesc("registered_at", "id")
        );
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> activityIds = registrations.stream()
                .map(ActivityRegistration::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Activity> activities = activityIds.isEmpty() ? Collections.emptyList() : activityMapper.selectBatchIds(activityIds);
        Map<Long, Activity> activityMap = activities.stream().collect(Collectors.toMap(Activity::getId, a -> a));

        List<RegistrationRecordView> result = new ArrayList<>(registrations.size());
        for (ActivityRegistration registration : registrations) {
            Activity activity = activityMap.get(registration.getActivityId());
            RegistrationRecordView view = new RegistrationRecordView();
            view.setRegistrationId(registration.getId());
            view.setActivityId(registration.getActivityId());
            view.setRegistrationStatus(registration.getStatus());
            view.setRegisteredAt(registration.getRegisteredAt());
            view.setCancelledAt(registration.getCancelledAt());
            view.setCheckinAt(registration.getCheckinAt());
            if (activity != null) {
                view.setActivityTitle(activity.getTitle());
                view.setLocation(activity.getLocation());
                view.setActivityStartTime(activity.getStartTime());
                view.setActivityEndTime(activity.getEndTime());
                view.setActivityStatus(activityPhaseResolver.resolve(activity));
            }
            result.add(view);
        }
        return result;
    }

    /**
     * 判断是否存在可更新字段，避免空更新。
     */
    private boolean hasAnyUpdatableField(UserProfileUpdateRequest request) {
        return StringUtils.hasText(request.getNickname())
                || StringUtils.hasText(request.getAvatarUrl())
                || StringUtils.hasText(request.getPhone())
                || request.getGender() != null;
    }
}
