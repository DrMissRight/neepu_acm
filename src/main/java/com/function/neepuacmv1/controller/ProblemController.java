package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.service.ProblemService;
import org.springframework.web.bind.annotation.*;

/** 题目控制器（题库表格/详情） */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public Result page(@RequestParam int page,
                       @RequestParam int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false) Long tagId) {
        return problemService.pageProblems(page, size, keyword, sort, tagId);
    }

    @GetMapping("/{problemId}")
    public Result detail(@PathVariable Long problemId,
                         @RequestParam(required = false) Integer recentLimit) {
        return problemService.getProblemDetail(problemId, recentLimit);
    }
}
