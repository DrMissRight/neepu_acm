package com.function.neepuacmv1.service;


import com.function.neepuacmv1.dto.req.SubmitReq;
import com.function.neepuacmv1.entity.Result;

public interface SubmitService {
    Result submit(SubmitReq req);
}
