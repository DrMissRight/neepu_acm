package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminProblemStatementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/problem-statements")
@RequireRoles({"ADMIN","COACH"})
public class AdminProblemStatementController {

    private final AdminProblemStatementService statementService;

    public AdminProblemStatementController(AdminProblemStatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping
    public Result list(@RequestParam Long problemId) {
        return statementService.listByProblem(problemId);
    }

    @PostMapping
    public Result create(@RequestBody AdminStatementCreateReq req) {
        return statementService.create(req);
    }

    @PutMapping
    public Result update(@RequestBody AdminStatementUpdateReq req) {
        return statementService.update(req);
    }

    @PutMapping("/toggle")
    public Result toggle(@RequestBody AdminStatementToggleReq req) {
        return statementService.toggle(req);
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody AdminBatchIdsReq req) {
        return statementService.deleteBatch(req);
    }
}
