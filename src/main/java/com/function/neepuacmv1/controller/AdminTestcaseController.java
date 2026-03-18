package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminTestcaseSaveReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.security.RequireRoles;
import com.function.neepuacmv1.service.AdminTestcaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/testcases")
@RequireRoles({"ADMIN","COACH"})
public class AdminTestcaseController {

    private final AdminTestcaseService testcaseService;

    public AdminTestcaseController(AdminTestcaseService testcaseService) {
        this.testcaseService = testcaseService;
    }

    @GetMapping
    public Result list(@RequestParam Long problemId) {
        return testcaseService.list(problemId);
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam Long problemId,
                         @RequestPart("files") MultipartFile[] files) {
        return testcaseService.upload(problemId, files);
    }

    @PutMapping("/{testcaseId}/sample")
    public Result setSample(@PathVariable Long testcaseId, @RequestParam Integer isSample) {
        return testcaseService.setSample(testcaseId, isSample);
    }

    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody AdminBatchIdsReq req) {
        return testcaseService.deleteBatch(req);
    }

    @PutMapping("/save")
    public Result save(@RequestBody AdminTestcaseSaveReq req) {
        return testcaseService.save(req);
    }

    /** 批量下载：Controller 仅负责把 Service 生成的 zipPath 输出为下载流 */
    @PostMapping("/download")
    public void download(@RequestBody AdminBatchIdsReq req, HttpServletResponse response) throws IOException {
        Result r = testcaseService.buildDownloadZip(req);
        if (r == null || r.getSuccess() == null || !r.getSuccess()) {
            response.setStatus(400);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"下载失败\",\"data\":null,\"total\":null}");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String,Object> data = (Map<String,Object>) r.getData();
        String zipPath = data == null ? null : String.valueOf(data.get("zipPath"));
        if (zipPath == null) {
            response.setStatus(400);
            return;
        }

        Path p = Path.of(zipPath);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"testcases.zip\"");
        try (InputStream in = Files.newInputStream(p);
             OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }
}
