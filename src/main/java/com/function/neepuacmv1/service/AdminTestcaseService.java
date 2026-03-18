package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminTestcaseSaveReq;
import com.function.neepuacmv1.entity.Result;
import org.springframework.web.multipart.MultipartFile;

public interface AdminTestcaseService {
    Result list(Long problemId);

    /** 上传：支持多文件/zip */
    Result upload(Long problemId, MultipartFile[] files);

    /** 设置/取消样例 */
    Result setSample(Long testcaseId, Integer isSample);

    /** 单删/批删 */
    Result deleteBatch(AdminBatchIdsReq req);

    /** 保存顺序/属性 */
    Result save(AdminTestcaseSaveReq req);

    /** 批量下载：返回 zip 二进制（这里返回的是下载 token/路径，Controller 再输出） */
    Result buildDownloadZip(AdminBatchIdsReq req);
}
