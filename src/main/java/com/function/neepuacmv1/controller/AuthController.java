package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.LoginReq;
import com.function.neepuacmv1.dto.req.RegisterReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.service.AuthService;
import org.springframework.web.bind.annotation.*;

/** 认证控制器：仅转发 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterReq req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginReq req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "X-Token", required = false) String token,
                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Controller 不做逻辑：把 token 原样交给 Service（Service 内可兼容 Bearer，这里简化）
        String t = token;
        if (t == null && authorization != null && authorization.startsWith("Bearer ")) {
            t = authorization.substring("Bearer ".length()).trim();
        }
        return authService.logout(t);
    }

    @GetMapping("/captcha")
    public Result captcha() {
        return authService.generateCaptcha();
    }

    @PostMapping("/verify-code")
    public Result sendVerifyCode(@RequestParam("verifyKey") String verifyKey) {
        return authService.sendVerifyCode(verifyKey);
    }
}
