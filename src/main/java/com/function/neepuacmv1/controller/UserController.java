package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.ChangePasswordReq;
import com.function.neepuacmv1.dto.req.UpdateProfileReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.UserService;
import org.springframework.web.bind.annotation.*;

/** 用户控制器：仅转发 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    @RequireRoles({"ADMIN","COACH","MEMBER","TRAINEE","GUEST"})
    public Result me() {
        return userService.currentUser();
    }

    @PutMapping("/profile")
    @RequireRoles({"ADMIN","COACH","MEMBER","TRAINEE","GUEST"})
    public Result updateProfile(@RequestBody UpdateProfileReq req) {
        return userService.updateProfile(req);
    }

    @PutMapping("/password")
    @RequireRoles({"ADMIN","COACH","MEMBER","TRAINEE","GUEST"})
    public Result changePassword(@RequestBody ChangePasswordReq req) {
        return userService.changePassword(req);
    }
}
