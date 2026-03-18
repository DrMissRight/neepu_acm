package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 当前用户信息 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResp {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String school;
    private String college;
    private String signature;
    private Integer status;
    private List<String> roles;
}
