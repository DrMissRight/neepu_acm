package com.function.neepuacmv1.dto.req;


import lombok.Data;

/** 管理员重置密码 */
@Data
public class AdminResetPasswordReq {
    private Long userId;
    private String newPassword;
}
