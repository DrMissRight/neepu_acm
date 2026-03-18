package com.function.neepuacmv1.dto.req;

import lombok.Data;

@Data
public class AdminContestProblemUpdateReq {
    private Long id;          // contest_problem.id
    private Long contestId;

    private String alias;
    private Integer weight;
    private String balloonColor;
    private Integer orderIndex;

    // 题面选择：
    // BASE：引用题库题面（baseStatementId）
    // CUSTOM：比赛专属题面（customStatementReq 另走接口）
    private String statementMode;   // BASE/CUSTOM
    private Long baseStatementId;   // BASE 时填写
}
