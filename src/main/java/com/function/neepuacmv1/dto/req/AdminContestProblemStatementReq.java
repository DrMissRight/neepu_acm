package com.function.neepuacmv1.dto.req;

import lombok.Data;

@Data
public class AdminContestProblemStatementReq {
    private Long contestId;
    private Long contestProblemId;

    /** true=设置自定义题面；false=清除自定义题面并回到 BASE */
    private Boolean enableCustom;

    // enableCustom=true 时生效
    private String lang;
    private String title;
    private String contentMd;
}
