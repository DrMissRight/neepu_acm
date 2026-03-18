package com.function.neepuacmv1.dto.req;

import lombok.Data;

@Data
public class AdminContestProblemAddReq {
    private Long contestId;
    /** 题库题号，如 P1000 */
    private String problemCode;
    /** 别名 A/B/C，可空，空则后端自动生成 */
    private String alias;
}
