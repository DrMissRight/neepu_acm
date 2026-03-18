package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 管理端题目详情（基本信息） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProblemDetailResp {
    private Long id;
    private String problemCode;
    private String title;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Integer difficulty;
    private String source;
    private Integer isPublic;
    private Long judgeTemplateId;
}
