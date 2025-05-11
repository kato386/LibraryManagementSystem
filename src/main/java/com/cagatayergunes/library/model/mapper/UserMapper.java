package com.cagatayergunes.library.model.mapper;

import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.model.request.UserRequest;
import com.cagatayergunes.library.model.response.FeedbackResponse;
import com.cagatayergunes.library.model.response.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .accountLocked(user.isAccountLocked())
                .enabled(user.isEnabled())
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getUpdatedDate())
                .roles(user.getRoles()
                        .stream()
                        .map(role -> role.getName().name())
                        .toList()
                )
                .build();
    }
}
