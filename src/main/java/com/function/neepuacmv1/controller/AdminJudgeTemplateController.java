package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminJudgeTemplateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/judge-templates")
@RequireRoles({"ADMIN","COACH"})
public class AdminJudgeTemplateController {

    private final AdminJudgeTemplateService service;

    public AdminJudgeTemplateController(AdminJudgeTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public Result page(@RequestParam int page,
                       @RequestParam int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String lang) {
        return service.page(page, size, keyword, type, lang);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    public Result create(@RequestBody AdminJudgeTemplateCreateReq req) {
        return service.create(req);
    }

    @PutMapping
    public Result update(@RequestBody AdminJudgeTemplateUpdateReq req) {
        return service.update(req);
    }

    @PostMapping("/fork")
    public Result fork(@RequestBody AdminJudgeTemplateForkReq req) {
        return service.fork(req);
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody AdminBatchIdsReq req) {
        return service.deleteBatch(req);
    }

    /** 供题库管理选择评测模板用：仅 enabled & public */
    @GetMapping("/options")
    public Result options() {
        return service.options();
    }
}
