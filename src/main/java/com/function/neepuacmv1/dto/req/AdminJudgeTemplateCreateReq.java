package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 新增评测模板 */
@Data
public class AdminJudgeTemplateCreateReq {
    private String name;
    private String type;       // IO/SPJ/Advanced
    private String lang;       // 可空
    private String scriptJson; // 必填
    private String description;
    private Integer isEnabled; // 1/0
    private Integer isPublic;  // 1/0
}
