package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminStatementCreateReq;
import com.function.neepuacmv1.dto.req.AdminStatementToggleReq;
import com.function.neepuacmv1.dto.req.AdminStatementUpdateReq;
import com.function.neepuacmv1.entity.Result;

public interface AdminProblemStatementService {
    Result listByProblem(Long problemId);
    Result create(AdminStatementCreateReq req);
    Result update(AdminStatementUpdateReq req);
    Result toggle(AdminStatementToggleReq req);
    Result deleteBatch(AdminBatchIdsReq req);
}
