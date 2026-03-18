package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 题目详情页聚合返回 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailResp {
    private Long id;
    private String problemCode;
    private String title;

    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Integer difficulty;
    private String source;

    private Integer acceptedCount;
    private Integer submittedCount;

    /** 默认题面（Description） */
    private ProblemStatementResp description;

    /** 多版本题面（Descriptions） */
    private List<ProblemStatementResp> descriptions;

    /** 标签 */
    private List<String> tags;

    /** 当前用户最近提交（Recent Submissions） */
    private List<RecentSubmissionResp> recentSubmissions;

    /** 当前用户是否已AC */
    private Boolean solved;
}
