package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.SubmissionQueryReq;
import com.function.neepuacmv1.entity.Result;

public interface SubmissionService {
    Result page(SubmissionQueryReq req);
    Result detail(Long id);
    Result recentByProblem(Long problemId, Integer limit);
}
