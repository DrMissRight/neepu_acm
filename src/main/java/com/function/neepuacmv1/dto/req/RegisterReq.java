package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 注册请求 */
@Data
public class RegisterReq {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;

    /** 可选：验证码（短信/邮箱/图形均可复用） */
    private String verifyKey;
    private String verifyCode;
}
