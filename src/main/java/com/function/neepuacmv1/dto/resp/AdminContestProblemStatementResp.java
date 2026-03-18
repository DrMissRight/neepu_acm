package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContestProblemStatementResp {
    private Long contestId;
    private Long contestProblemId;
    private String statementMode; // BASE/CUSTOM
    private Long baseStatementId;
    private Long customStatementId;

    // CUSTOM 详情（若存在）
    private String lang;
    private String title;
    private String contentMd;
}
