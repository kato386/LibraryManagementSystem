package com.cagatayergunes.library.controller;

import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.model.request.UpdateRoleRequest;
import com.cagatayergunes.library.model.request.UserRequest;
import com.cagatayergunes.library.model.response.UserResponse;
import com.cagatayergunes.library.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {

    private final UserService service;

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ResponseEntity.ok(service.updateUser(id, request));
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest roleName) {
        service.updateUserRole(id, roleName);
        return ResponseEntity.ok("Role updated successfully.");
    }
}
