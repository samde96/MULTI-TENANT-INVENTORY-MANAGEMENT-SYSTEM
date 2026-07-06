package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.AppRole;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.Location;
import com.keen.erp.repo.AppRoleRepository;
import com.keen.erp.repo.AppUserRepository;
import com.keen.erp.repo.LocationRepository;
import com.keen.erp.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final LocationRepository locationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public UserAdminService(AppUserRepository appUserRepository,
                            AppRoleRepository appRoleRepository,
                            LocationRepository locationRepository,
                            CurrentUserService currentUserService,
                            AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.locationRepository = locationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.PendingUserResponse> pendingUsers() {
        currentUserService.requirePermission("admin:all");
        return appUserRepository.findAllByActiveFalseOrderByCreatedAtDesc().stream()
                .map(user -> new ApiDtos.PendingUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.ApprovalOptionsResponse options() {
        currentUserService.requirePermission("admin:all");
        List<ApiDtos.RoleResponse> roles = appRoleRepository.findAll().stream()
                .sorted(Comparator.comparing(AppRole::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(role -> new ApiDtos.RoleResponse(role.getId(), role.getCode(), role.getName(), role.getPermissions()))
                .toList();
        List<ApiDtos.LocationResponse> locations = locationRepository.findAll().stream()
                .filter(Location::isActive)
                .sorted(Comparator.comparing(Location::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(location -> new ApiDtos.LocationResponse(location.getId(), location.getCode(), location.getName(), location.getType(), location.isActive()))
                .toList();
        return new ApiDtos.ApprovalOptionsResponse(roles, locations);
    }

    @Transactional
    public ApiDtos.ApprovalResponse approve(Long userId, ApiDtos.ApproveUserRequest request) {
        currentUserService.requirePermission("admin:all");

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        if (user.isActive()) {
            throw new IllegalStateException("Account already approved");
        }

        Set<Long> roleIds = request.roleIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one role");
        }

        List<AppRole> roles = resolveRoles(roleIds);
        List<Location> locations = request.locationIds() == null || request.locationIds().isEmpty()
                ? List.of()
                : resolveLocations(request.locationIds().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        String before = summarize(user);
        user.setRoles(new LinkedHashSet<>(roles));
        user.setLocations(new LinkedHashSet<>(locations));
        user.setActive(true);

        AppUser saved = appUserRepository.save(user);
        auditService.record("USER_APPROVE", "USER", String.valueOf(saved.getId()), before, summarize(saved), true);

        return new ApiDtos.ApprovalResponse(
                saved.getId(),
                saved.getUsername(),
                true,
                "Account approved"
        );
    }

    private List<AppRole> resolveRoles(Set<Long> roleIds) {
        List<AppRole> roles = new ArrayList<>();
        appRoleRepository.findAllById(roleIds).forEach(roles::add);
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("One or more roles were not found");
        }
        return roles;
    }

    private List<Location> resolveLocations(Set<Long> locationIds) {
        List<Location> locations = new ArrayList<>();
        locationRepository.findAllById(locationIds).forEach(location -> {
            if (!location.isActive()) {
                throw new IllegalArgumentException("Inactive location cannot be assigned: " + location.getCode());
            }
            locations.add(location);
        });
        if (locations.size() != locationIds.size()) {
            throw new IllegalArgumentException("One or more locations were not found");
        }
        return locations;
    }

    private String summarize(AppUser user) {
        String roles = user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(AppRole::getCode)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
        String locations = user.getLocations().stream()
                .filter(Objects::nonNull)
                .map(Location::getCode)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
        return "username=" + user.getUsername()
                + ",active=" + user.isActive()
                + ",roles=" + roles
                + ",locations=" + locations;
    }
}
