package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminProblemCreateReq;
import com.function.neepuacmv1.dto.req.AdminProblemUpdateReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminProblemService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/problems")
@RequireRoles({"ADMIN","COACH"})
public class AdminProblemController {

    private final AdminProblemService problemService;

    public AdminProblemController(AdminProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public Result page(@RequestParam int page,
                       @RequestParam int size,
                       @RequestParam(required = false) String keyword) {
        return problemService.page(page, size, keyword);
    }

    @GetMapping("/{problemId}")
    public Result detail(@PathVariable Long problemId) {
        return problemService.detail(problemId);
    }

    @PostMapping
    public Result create(@RequestBody AdminProblemCreateReq req) {
        return problemService.create(req);
    }

    @PutMapping
    public Result update(@RequestBody AdminProblemUpdateReq req) {
        return problemService.update(req);
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody AdminBatchIdsReq req) {
        return problemService.deleteBatch(req);
    }
}
