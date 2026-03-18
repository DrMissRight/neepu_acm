package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContestProblemResp {
    private Long id;
    private Long contestId;
    private Long problemId;
    private String problemCode;

    private String alias;
    private Integer weight;
    private String balloonColor;
    private Integer orderIndex;

    private String statementMode;
    private Long baseStatementId;
    private Long customStatementId;
}
