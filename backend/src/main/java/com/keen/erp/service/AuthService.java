package com.keen.erp.service;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.entity.AppRole;
import com.keen.erp.entity.AppUser;
import com.keen.erp.entity.Location;
import com.keen.erp.entity.NotificationType;
import com.keen.erp.repo.AppUserRepository;
import com.keen.erp.security.CurrentUserService;
import com.keen.erp.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppUserRepository appUserRepository,
                       CurrentUserService currentUserService,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       NotificationService notificationService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public ApiDtos.AuthResponse login(ApiDtos.LoginRequest request) {
        String username = request.username().trim();
        AppUser existingUser = appUserRepository.findByUsernameIgnoreCase(username)
                .orElse(null);
        if (existingUser != null && !existingUser.isActive()) {
            throw new DisabledException("Account pending approval");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));
        AppUser user = appUserRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return new ApiDtos.AuthResponse(jwtService.generateToken(user), toProfile(user));
    }

    @Transactional
    public ApiDtos.RegistrationResponse register(ApiDtos.RegisterRequest request) {
        String username = request.username().trim();
        String fullName = request.fullName().trim();
        String email = request.email().trim();

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("Username already exists");
        }
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(false);
        user.setRoles(new LinkedHashSet<>());
        user.setLocations(new LinkedHashSet<>());

        AppUser saved = appUserRepository.save(user);

        String summary = "username=" + saved.getUsername() + ",email=" + saved.getEmail() + ",status=PENDING_APPROVAL";
        auditService.record("USER_REGISTER", "USER", String.valueOf(saved.getId()), null, summary, true);
        notificationService.create(null, "ADMIN", NotificationType.INFO, "IN_APP",
                "New self-registration",
                saved.getFullName() + " (" + saved.getUsername() + ") submitted a new account request and is awaiting approval.",
                "USER-" + saved.getId());

        return new ApiDtos.RegistrationResponse(
                saved.getId(),
                saved.getUsername(),
                true,
                "Registration received. Your account is pending approval."
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.UserProfileResponse me() {
        return toProfile(currentUserService.currentUser());
    }

    public ApiDtos.UserProfileResponse toProfile(AppUser user) {
        Set<String> permissions = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        List<ApiDtos.RoleResponse> roles = user.getRoles().stream()
                .map(role -> new ApiDtos.RoleResponse(role.getId(), role.getCode(), role.getName(), role.getPermissions()))
                .toList();
        List<ApiDtos.LocationResponse> locations = user.getLocations().stream()
                .map(location -> new ApiDtos.LocationResponse(location.getId(), location.getCode(), location.getName(), location.getType(), location.isActive()))
                .toList();
        return new ApiDtos.UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                permissions,
                roles,
                locations
        );
    }
}
