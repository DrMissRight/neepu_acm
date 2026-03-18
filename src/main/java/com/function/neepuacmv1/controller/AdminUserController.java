package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.AdminAssignRolesReq;
import com.function.neepuacmv1.dto.req.AdminCreateUserReq;
import com.function.neepuacmv1.dto.req.AdminResetPasswordReq;
import com.function.neepuacmv1.dto.req.AdminUpdateUserReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

/** 管理员用户管理：仅转发 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRoles({"ADMIN"})
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Result page(@RequestParam int page,
                       @RequestParam int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String roleCode,
                       @RequestParam(required = false) Integer status) {
        return adminUserService.pageUsers(page, size, keyword, roleCode, status);
    }

    @PostMapping
    public Result create(@RequestBody AdminCreateUserReq req) {
        return adminUserService.createUser(req);
    }

    @PutMapping
    public Result update(@RequestBody AdminUpdateUserReq req) {
        return adminUserService.updateUser(req);
    }

    @PutMapping("/password")
    public Result resetPassword(@RequestBody AdminResetPasswordReq req) {
        return adminUserService.resetPassword(req);
    }

    @PutMapping("/roles")
    public Result assignRoles(@RequestBody AdminAssignRolesReq req) {
        return adminUserService.assignRoles(req);
    }

    @DeleteMapping("/{userId}")
    public Result delete(@PathVariable Long userId) {
        return adminUserService.deleteUser(userId);
    }

    @PutMapping("/{userId}/status")
    public Result enable(@PathVariable Long userId, @RequestParam Integer status) {
        return adminUserService.enableUser(userId, status);
    }

    @PostMapping("/batch-delete")
    public Result batchDelete(@RequestBody com.function.neepuacmv1.dto.req.AdminBatchDeleteReq req) {
        return adminUserService.batchDelete(req);
    }

    @PostMapping("/import")
    public Result importUsers(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return adminUserService.importUsers(file);
    }
}
