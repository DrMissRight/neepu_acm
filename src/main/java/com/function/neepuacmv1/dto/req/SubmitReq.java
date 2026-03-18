package com.function.neepuacmv1.dto.req;

import lombok.Data;

@Data
public class SubmitReq {
    private Long problemId;
    private Long contestId;      // 可空
    private String language;     // cpp11/java8/python3...
    private String code;         // 源码
}
