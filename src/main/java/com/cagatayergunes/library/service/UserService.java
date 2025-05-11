package com.cagatayergunes.library.service;

import com.cagatayergunes.library.model.Role;
import com.cagatayergunes.library.model.RoleName;
import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.model.mapper.UserMapper;
import com.cagatayergunes.library.model.request.UpdateRoleRequest;
import com.cagatayergunes.library.model.request.UserRequest;
import com.cagatayergunes.library.model.response.UserResponse;
import com.cagatayergunes.library.repository.RoleRepository;
import com.cagatayergunes.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;

    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = getUser(id);
        log.info("User found: {}", user.getEmail());
        return mapper.toUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users.");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(mapper::toUserResponse)
                .toList();
        log.debug("Found {} users", users.size());
        return users;
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user with id: {}", id);
        User user = getUser(id);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAccountLocked(request.accountLocked());
        user.setEmail(request.email());

        User updatedUser = userRepository.save(user);
        log.info("User with id: {} updated successfully", id);
        return mapper.toUserResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = getUser(id);
        userRepository.delete(user);
        log.info("User with id: {} deleted successfully", id);
    }

    public void updateUserRole(Long userId, UpdateRoleRequest request) {
        log.info("Updating role for user with id: {}. New role: {}", userId, request.roleName());
        User user = getUser(userId);

        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));

        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Role {} added to user with id: {}", request.roleName(), userId);
    }

    private User getUser(Long id){
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User with id: {} not found", id);
                    return new EntityNotFoundException("User not found with id: " + id);
                });
        log.info("User found: {}", user.getEmail());
        return user;
    }
}
