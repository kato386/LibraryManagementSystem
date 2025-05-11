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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;

    public UserResponse getUserById(Long id) {
        User user =  getUser(id);
        return mapper.toUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().
                stream()
                .map(mapper::toUserResponse)
                .toList();
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user =  getUser(id);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAccountLocked(request.accountLocked());
        user.setEmail(request.email());

        return mapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    public void updateUserRole(Long userId, UpdateRoleRequest request) {
        RoleName roleName = request.roleName();
        User user = getUser(userId);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    private User getUser(Long id){
        User user =  userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return user;
    }
}
