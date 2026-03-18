package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 题面列表中的开关：默认/公开 */
@Data
public class AdminStatementToggleReq {
    private Long id;
    private Integer isDefault; // nullable
    private Integer isPublic;  // nullable
}
