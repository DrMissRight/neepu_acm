package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 新增题面 */
@Data
public class AdminStatementCreateReq {
    private Long problemId;
    private String lang;         // zh-CN / en
    private String versionName;  // Default / Short / ...
    private String title;        // 列表可编辑
    private String contentMd;    // markdown/html
    private Integer isPublic;    // 1/0
    private Integer isDefault;   // 1/0
}
