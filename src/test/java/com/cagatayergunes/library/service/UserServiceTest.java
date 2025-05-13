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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserMapper mapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        mapper = mock(UserMapper.class);
        userService = new UserService(userRepository, roleRepository, mapper);
    }

    @Test
    void getUserById_ShouldReturnUserResponse() {
        User user = getMockUser();
        UserResponse response = new UserResponse(1L, "John", "Doe", "john@example.com", false, false, LocalDateTime.now(),LocalDateTime.now(), List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mapper.toUserResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserById(1L);

        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUserById_ShouldThrowException_IfNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getAllUsers_ShouldReturnAllMappedUsers() {
        List<User> users = List.of(getMockUser());
        List<UserResponse> responses = List.of(new UserResponse(1L, "John", "Doe", "john@example.com", false, false, LocalDateTime.now(),LocalDateTime.now(), List.of()));

        when(userRepository.findAll()).thenReturn(users);
        when(mapper.toUserResponse(any(User.class))).thenReturn(responses.get(0));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void updateUser_ShouldUpdateAndReturnUser() {
        User user = getMockUser();
        User updated = getMockUser();
        updated.setFirstName("Updated");
        UserResponse response = new UserResponse(1L, "Updated", "Doe", "john@example.com", false, false, LocalDateTime.now(),LocalDateTime.now(), List.of());
        UserRequest request = new UserRequest("Updated", "Doe", "john@example.com", false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updated);
        when(mapper.toUserResponse(updated)).thenReturn(response);

        UserResponse result = userService.updateUser(1L, request);

        assertEquals("Updated", result.getFirstName());
    }

    @Test
    void deleteUser_ShouldCallDeleteMethod() {
        User user = getMockUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void updateUserRole_ShouldAddRoleToUser() {
        User user = getMockUser();
        Role role = Role.builder().id(1L).name(RoleName.PATRON).build();
        UpdateRoleRequest request = new UpdateRoleRequest(RoleName.PATRON);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.PATRON)).thenReturn(Optional.of(role));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateUserRole(1L, request);

        assertTrue(user.getRoles().contains(role));
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_ShouldThrowException_WhenRoleNotFound() {
        User user = getMockUser();
        UpdateRoleRequest request = new UpdateRoleRequest(RoleName.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserRole(1L, request));
    }

    private User getMockUser() {
        return User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .accountLocked(false)
                .enabled(false)
                .roles(new ArrayList<>())
                .build();
    }
}
