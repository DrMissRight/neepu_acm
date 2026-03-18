package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminProblemCreateReq;
import com.function.neepuacmv1.dto.req.AdminProblemUpdateReq;
import com.function.neepuacmv1.entity.Result;

public interface AdminProblemService {
    Result page(int page, int size, String keyword);
    Result detail(Long problemId);
    Result create(AdminProblemCreateReq req);
    Result update(AdminProblemUpdateReq req);
    Result deleteBatch(AdminBatchIdsReq req);
}
