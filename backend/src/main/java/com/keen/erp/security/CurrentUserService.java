package com.keen.erp.security;

import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.Location;
import com.keen.erp.repo.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required");
        }
        return appUserRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User account not found"));
    }

    public void requirePermission(String permission) {
        AppUser user = currentUser();
        if (!user.isAdmin() && !user.hasPermission(permission)) {
            throw new AccessDeniedException("Missing permission: " + permission);
        }
    }

    public void requireAnyPermission(String... permissions) {
        AppUser user = currentUser();
        if (user.isAdmin()) {
            return;
        }
        if (permissions == null || permissions.length == 0) {
            return;
        }
        for (String permission : permissions) {
            if (permission != null && user.hasPermission(permission)) {
                return;
            }
        }
        throw new AccessDeniedException("Missing required permission");
    }

    public boolean isAdmin() {
        return currentUser().isAdmin();
    }

    public Set<Long> currentUserLocationIds() {
        AppUser user = currentUser();
        if (user.isAdmin()) {
            return Set.of();
        }
        return user.getLocations().stream()
                .filter(Objects::nonNull)
                .map(Location::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean hasLocationAccess(Location location) {
        if (location == null) {
            return false;
        }
        AppUser user = currentUser();
        if (user.isAdmin()) {
            return true;
        }
        return user.getLocations().stream()
                .filter(Objects::nonNull)
                .anyMatch(userLocation -> Objects.equals(userLocation.getId(), location.getId()));
    }

    public void requireLocationAccess(Location location) {
        if (!hasLocationAccess(location)) {
            throw new AccessDeniedException("Location access denied: " + location.getName());
        }
    }

    public void requireAnyLocationAccess(Collection<Location> locations) {
        AppUser user = currentUser();
        if (user.isAdmin()) {
            return;
        }
        boolean allowed = locations != null && locations.stream().anyMatch(location ->
                user.getLocations().stream().anyMatch(userLocation -> Objects.equals(userLocation.getId(), location.getId())));
        if (!allowed) {
            throw new AccessDeniedException("Location access denied");
        }
    }
}
