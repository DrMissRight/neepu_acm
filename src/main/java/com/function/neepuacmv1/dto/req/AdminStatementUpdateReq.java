package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 编辑题面详情（保存） */
@Data
public class AdminStatementUpdateReq {
    private Long id;
    private String title;
    private String contentMd;
}
