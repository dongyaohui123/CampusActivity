package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.user.UserProfileUpdateRequest;
import com.campus.activity.dto.v1.user.UserPasswordChangeRequest;
import com.campus.activity.entity.User;
import com.campus.activity.view.v1.RegistrationRecordView;
import com.campus.activity.view.v1.AvatarUploadView;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * V1UserService服务接口。
 */
public interface V1UserService {
    /**
     * 获取用户资料。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 用户资料
     */
    User getUserProfile(Long userId, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);

    /**
     * 更新用户资料。
     *
     * @param userId 目标用户 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的用户资料
     */
    User updateUserProfile(Long userId, UserProfileUpdateRequest request, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);

    /**
     * 上传用户头像并返回公开访问地址。
     *
     * @param userId 目标用户 ID
     * @param file 头像文件
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 头像上传结果
     */
    AvatarUploadView uploadAvatar(Long userId, MultipartFile file, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);

    /**
     * 修改用户密码。
     *
     * @param userId 目标用户 ID
     * @param request 修改密码请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     */
    void changeUserPassword(Long userId, UserPasswordChangeRequest request, Long operatorUserId,
            com.campus.activity.enums.UserRole operatorRole);

    /**
     * 获取用户报名记录。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录列表
     */
    List<RegistrationRecordView> getUserRegistrations(Long userId, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);
}
