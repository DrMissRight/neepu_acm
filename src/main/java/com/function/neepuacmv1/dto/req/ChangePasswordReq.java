package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 修改密码 */
@Data
public class ChangePasswordReq {
    private String oldPassword;
    private String newPassword;
}
