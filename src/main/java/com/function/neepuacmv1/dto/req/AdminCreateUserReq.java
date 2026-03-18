package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

/** 管理员新增用户 */
@Data
public class AdminCreateUserReq {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;

    /** 角色编码列表：ADMIN/COACH/MEMBER/TRAINEE/GUEST */
    private List<String> roleCodes;

    /** 1启用 0禁用 */
    private Integer status;
}
