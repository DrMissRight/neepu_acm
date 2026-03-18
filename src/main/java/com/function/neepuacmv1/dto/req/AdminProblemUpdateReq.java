package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 修改题目基本信息 */
@Data
public class AdminProblemUpdateReq {
    private Long id;
    private String title;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Integer difficulty;
    private String source;
    private Integer isPublic;
    private Long judgeTemplateId;
}
