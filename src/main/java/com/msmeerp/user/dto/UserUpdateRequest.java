package com.msmeerp.user.dto;

import com.msmeerp.user.entity.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Partial update — every field is optional, only fields present are applied.
 * See {@code UserServiceImpl#updateUser}, which null-checks each field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;
    private User.UserStatus status;
    private Set<Long> roleIds;
}
