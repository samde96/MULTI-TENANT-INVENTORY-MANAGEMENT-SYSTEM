package com.keen.erp.controller;

import com.keen.erp.dto.ApiDtos;
import com.keen.erp.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/pending")
    public List<ApiDtos.PendingUserResponse> pendingUsers() {
        return userAdminService.pendingUsers();
    }

    @GetMapping("/options")
    public ApiDtos.ApprovalOptionsResponse options() {
        return userAdminService.options();
    }

    @PostMapping("/{id}/approve")
    public ApiDtos.ApprovalResponse approve(@PathVariable Long id,
                                            @Valid @RequestBody ApiDtos.ApproveUserRequest request) {
        return userAdminService.approve(id, request);
    }
}
