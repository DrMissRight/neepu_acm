package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminJudgeTemplateCreateReq;
import com.function.neepuacmv1.dto.req.AdminJudgeTemplateForkReq;
import com.function.neepuacmv1.dto.req.AdminJudgeTemplateUpdateReq;
import com.function.neepuacmv1.entity.Result;

public interface AdminJudgeTemplateService {
    Result page(int page, int size, String keyword, String type, String lang);

    Result detail(Long id);

    Result create(AdminJudgeTemplateCreateReq req);

    Result update(AdminJudgeTemplateUpdateReq req);

    Result fork(AdminJudgeTemplateForkReq req);

    Result deleteBatch(AdminBatchIdsReq req);

    Result options(); // 下拉选择(仅 enabled & public)
}
