package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 题库表格单行 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemListItemResp {
    private Long id;
    private String problemCode;
    private String title;
    private Integer acceptedCount;
    private Integer submittedCount;
    /** 已登录用户：是否已AC */
    private Boolean solved;
}
