package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionCaseResp {
    private Long testcaseId;
    private String status;
    private Integer timeMs;
    private Integer memoryKb;
    private Integer score;
    private String info;
}
