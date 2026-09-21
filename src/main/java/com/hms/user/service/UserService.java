package com.hms.user.service;

import com.hms.common.response.PagedResponse;
import com.hms.hospital.dto.BranchAdminCreateRequest;
import com.hms.user.dto.UserCreateRequest;
import com.hms.user.dto.UserResponse;
import com.hms.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse createBranchAdmin(BranchAdminCreateRequest request);
    UserResponse getUserById(Long id);
    UserResponse getUserByUsername(String username);
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
}
