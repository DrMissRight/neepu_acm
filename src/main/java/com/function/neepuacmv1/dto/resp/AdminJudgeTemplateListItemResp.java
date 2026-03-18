package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 列表项 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminJudgeTemplateListItemResp {
    private Long id;
    private String name;
    private String type;
    private String lang;
    private Integer isEnabled;
    private Integer isPublic;
    private Long forkFromId;
    private LocalDateTime updatedAt;
}
