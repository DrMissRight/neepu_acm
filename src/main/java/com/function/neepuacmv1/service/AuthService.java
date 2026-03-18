package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.LoginReq;
import com.function.neepuacmv1.dto.req.RegisterReq;
import com.function.neepuacmv1.entity.Result;

/** 认证服务 */
public interface AuthService {

    Result register(RegisterReq req);

    Result login(LoginReq req);

    Result logout(String token);

    /** 发送验证码（短信/邮箱等）：这里只实现 Redis 缓存，发送动作可对接第三方 */
    Result sendVerifyCode(String verifyKey);

    /** 图形验证码：返回 captchaId + code(生产环境不返回code，这里可用开关控制) */
    Result generateCaptcha();
}
