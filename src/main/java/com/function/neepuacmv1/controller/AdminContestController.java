package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminContestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contests")
@RequireRoles({"ADMIN","COACH"})
public class AdminContestController {

    private final AdminContestService contestService;

    public AdminContestController(AdminContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public Result page(@RequestParam int page,
                       @RequestParam int size,
                       @RequestParam(required = false) String keyword) {
        return contestService.page(page, size, keyword);
    }

    @GetMapping("/{contestId}")
    public Result detail(@PathVariable Long contestId) {
        return contestService.detail(contestId);
    }

    @PostMapping
    public Result create(@RequestBody AdminContestCreateReq req) {
        return contestService.create(req);
    }

    @PutMapping
    public Result update(@RequestBody AdminContestUpdateReq req) {
        return contestService.update(req);
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody AdminBatchIdsReq req) {
        return contestService.deleteBatch(req);
    }

    @PutMapping("/participants")
    public Result updateParticipants(@RequestBody AdminContestParticipantsReq req) {
        return contestService.updateParticipants(req);
    }

    @GetMapping("/{contestId}/problems")
    public Result listProblems(@PathVariable Long contestId) {
        return contestService.listProblems(contestId);
    }

    @PostMapping("/problems")
    public Result addProblem(@RequestBody AdminContestProblemAddReq req) {
        return contestService.addProblem(req);
    }

    @PutMapping("/problems")
    public Result updateProblem(@RequestBody AdminContestProblemUpdateReq req) {
        return contestService.updateProblem(req);
    }

    @DeleteMapping("/{contestId}/problems/{contestProblemId}")
    public Result removeProblem(@PathVariable Long contestId, @PathVariable Long contestProblemId) {
        return contestService.removeProblem(contestId, contestProblemId);
    }

    @GetMapping("/problem-validate")
    public Result validateProblemCode(@RequestParam String problemCode) {
        return contestService.validateProblemCode(problemCode);
    }

    @GetMapping("/{contestId}/problems/{contestProblemId}/statement")
    public Result getStatement(@PathVariable Long contestId, @PathVariable Long contestProblemId) {
        return contestService.getContestProblemStatement(contestId, contestProblemId);
    }

    @PutMapping("/{contestId}/problems/{contestProblemId}/statement")
    public Result setStatement(@RequestBody AdminContestProblemStatementReq req) {
        return contestService.setContestProblemStatement(req);
    }
}
