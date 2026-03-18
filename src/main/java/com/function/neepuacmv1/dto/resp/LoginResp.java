package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 登录响应 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private List<String> roles;
}
