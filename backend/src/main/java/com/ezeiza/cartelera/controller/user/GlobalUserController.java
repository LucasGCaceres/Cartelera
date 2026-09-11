package com.ezeiza.cartelera.controller.user;

import com.ezeiza.cartelera.dto.user.CreateGlobalUserRequest;
import com.ezeiza.cartelera.dto.user.GlobalUserResponse;
import com.ezeiza.cartelera.dto.user.UpdateGlobalUserRequest;
import com.ezeiza.cartelera.dto.user.UpdateGlobalUserStatusRequest;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.service.user.GlobalUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/global")
public class GlobalUserController {

    private final GlobalUserService globalUserService;

    public GlobalUserController(GlobalUserService globalUserService) {
        this.globalUserService = globalUserService;
    }

    @GetMapping
    public List<GlobalUserResponse> getUsers() {
        return globalUserService.findAllUsers()
                .stream()
                .map(this::toGlobalUserResponse)
                .toList();
    }

    @PostMapping
    public GlobalUserResponse createUser(@RequestBody CreateGlobalUserRequest request) {
        AppUser user = globalUserService.createUser(
                request.username(),
                request.corporateEmail(),
                request.fullName(),
                request.password(),
                request.platformAdmin()
        );

        return toGlobalUserResponse(user);
    }

    @PatchMapping("/{userId}")
    public GlobalUserResponse updateUser(@PathVariable Long userId,
                                         @RequestBody UpdateGlobalUserRequest request) {
        AppUser user = globalUserService.updateUser(
                userId,
                request.corporateEmail(),
                request.fullName(),
                request.platformAdmin()
        );

        return toGlobalUserResponse(user);
    }

    @PatchMapping("/{userId}/status")
    public GlobalUserResponse updateStatus(@PathVariable Long userId,
                                           @RequestBody UpdateGlobalUserStatusRequest request) {
        boolean active = Boolean.TRUE.equals(request.active());

        AppUser user = globalUserService.updateStatus(
                userId,
                active
        );

        return toGlobalUserResponse(user);
    }

    private GlobalUserResponse toGlobalUserResponse(AppUser user) {
        return new GlobalUserResponse(
                user.getId(),
                user.getUsername(),
                user.getCorporateEmail(),
                user.getFullName(),
                user.isActive(),
                user.isPlatformAdmin(),
                user.getEntraObjectId(),
                user.getEntraTenantId(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
        );
    }
}