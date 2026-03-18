package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

/** 批量操作 id 列表 */
@Data
public class AdminBatchIdsReq {
    private List<Long> ids;
}
