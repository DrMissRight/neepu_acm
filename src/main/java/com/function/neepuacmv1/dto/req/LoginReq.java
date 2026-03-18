package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 登录请求 */
@Data
public class LoginReq {
    /** username/email/phone 均可 */
    private String account;
    private String password;

    /** 可选：图形验证码 */
    private String captchaId;
    private String captchaCode;
}
