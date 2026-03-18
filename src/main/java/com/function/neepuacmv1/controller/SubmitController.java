package com.function.neepuacmv1.controller;


import com.function.neepuacmv1.dto.req.SubmitReq;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.service.SubmitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class SubmitController {

    private final SubmitService submitService;

    public SubmitController(SubmitService submitService) {
        this.submitService = submitService;
    }

    @PostMapping("/submit")
    public Result submit(@RequestBody SubmitReq req) {
        return submitService.submit(req);
    }
}
