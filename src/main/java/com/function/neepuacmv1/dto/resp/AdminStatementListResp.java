package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 管理端题面列表项 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatementListResp {
    private Long id;
    private Long problemId;
    private String lang;
    private String versionName;
    private String title;
    private Integer isDefault;
    private Integer isPublic;
    private LocalDateTime updatedAt;
}
