package com.msmeerp.user.service;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.common.response.PagedResponse;
import com.msmeerp.user.dto.AcceptUserInviteRequest;
import com.msmeerp.user.dto.UserCreateRequest;
import com.msmeerp.user.dto.UserInviteRequest;
import com.msmeerp.user.dto.UserResponse;
import com.msmeerp.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse getUserById(Long id);
    UserResponse getUserByUsername(String username);
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    UserResponse inviteUser(UserInviteRequest request);
    UserResponse resendInvite(Long userId);
    ApiResponse<Void> acceptInvite(AcceptUserInviteRequest request);
}
