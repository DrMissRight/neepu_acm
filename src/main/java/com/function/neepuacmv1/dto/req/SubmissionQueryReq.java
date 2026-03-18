package com.function.neepuacmv1.dto.req;

import lombok.Data;

/**
 * Submission 列表查询条件（对应 SDUOJ：用户/题目/结果/语言检索 + 时间/内存排序）
 */
@Data
public class SubmissionQueryReq {
    private Integer page;
    private Integer size;

    /** 用户：支持 username/account 模糊（实现里按你 user 表字段调整） */
    private String user;

    /** 题目：支持 problemCode 模糊/精确 */
    private String problem;

    /** 结果：AC/WA/TLE... */
    private String status;

    /** 语言：cpp11/java8/python3... */
    private String language;

    /** 排序字段：time/memory/createdAt */
    private String sortBy;

    /** asc/desc */
    private String order;
}
