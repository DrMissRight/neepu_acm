package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 管理员编辑用户基础信息 */
@Data
public class AdminUpdateUserReq {
    private Long userId;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String school;
    private String college;
    private String signature;
    private Integer status; // 可同时改启用/禁用
}
