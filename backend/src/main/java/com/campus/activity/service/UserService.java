package com.campus.activity.service;

import com.campus.activity.common.PageResponse;
import com.campus.activity.dto.user.CreateUserRequest;
import com.campus.activity.dto.user.UpdateUserRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;

public interface UserService {
    User createUser(CreateUserRequest request);

    User updateUser(Long id, UpdateUserRequest request);

    void disableUser(Long id);

    User getUserById(Long id);

    PageResponse<User> listUsers(String username, UserStatus status, UserRole role, long page, long size);
}
