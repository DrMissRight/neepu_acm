package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 题面版本 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemStatementResp {
    private Long id;
    private String lang;
    private String versionName;
    private String contentMd;
    private Boolean isDefault;
}
