package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 编辑评测模板 */
@Data
public class AdminJudgeTemplateUpdateReq {
    private Long id;
    private String name;
    private String type;       // 一般不建议改，但允许
    private String lang;
    private String scriptJson;
    private String description;
    private Integer isEnabled;
    private Integer isPublic;
}
