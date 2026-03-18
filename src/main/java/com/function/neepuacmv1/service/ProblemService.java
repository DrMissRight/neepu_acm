package com.function.neepuacmv1.service;

import com.function.neepuacmv1.entity.Result;

public interface ProblemService {

    /** 题库表格分页：可按通过数排序；已登录返回 solved */
    Result pageProblems(int page, int size, String keyword, String sort, Long tagId);

    /** 题目详情：题面/信息/多版本/标签/最近提交 */
    Result getProblemDetail(Long problemId, Integer recentLimit);
}
