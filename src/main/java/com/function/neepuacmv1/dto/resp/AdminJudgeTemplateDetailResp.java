package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 详情 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminJudgeTemplateDetailResp {
    private Long id;
    private String name;
    private String type;
    private String lang;
    private String scriptJson;
    private String description;
    private Integer isEnabled;
    private Integer isPublic;
    private Long forkFromId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
