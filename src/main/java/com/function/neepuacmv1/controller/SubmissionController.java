package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.SubmissionQueryReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.service.SubmissionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** Submission 列表（全站） */
    @PostMapping("/page")
    public Result page(@RequestBody SubmissionQueryReq req) {
        return submissionService.page(req);
    }

    /** Submission 详情 */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return submissionService.detail(id);
    }

    /** 题目页 Recent Submissions */
    @GetMapping("/recent")
    public Result recentByProblem(@RequestParam Long problemId,
                                  @RequestParam(required = false) Integer limit) {
        return submissionService.recentByProblem(problemId, limit);
    }
}
