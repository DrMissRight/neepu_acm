package com.function.neepuacmv1.dto.req;

import lombok.Data;

/** 新增题目 */
@Data
public class AdminProblemCreateReq {
    private String problemCode;      // P1000
    private String title;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Integer difficulty;
    private String source;
    private Integer isPublic;        // 1/0
    private Long judgeTemplateId;    // 可选
}
