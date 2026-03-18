package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

/** 管理员分配角色 */
@Data
public class AdminAssignRolesReq {
    private Long userId;
    private List<String> roleCodes;
}