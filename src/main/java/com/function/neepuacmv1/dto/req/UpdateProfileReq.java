package com.function.neepuacmv1.dto.req;

import lombok.Data;

/**
 * 更新个人信息
 */
@Data
public class UpdateProfileReq {
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String school;
    private String college;
    private String signature;
}
