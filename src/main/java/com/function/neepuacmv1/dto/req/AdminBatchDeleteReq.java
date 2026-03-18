package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

/** 管理员批量删除用户 */
@Data
public class AdminBatchDeleteReq {
    private List<Long> userIds;
}
