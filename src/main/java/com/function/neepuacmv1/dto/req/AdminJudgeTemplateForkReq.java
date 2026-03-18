package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** Fork：基于模板复制一份 */
@Data
public class AdminJudgeTemplateForkReq {
    private Long sourceId;
    private String newName;      // 新模板名
    private String newLang;      // 可选
    private String newDescription;
    private Integer isPublic;    // 可选
    private Integer isEnabled;   // 可选
}
