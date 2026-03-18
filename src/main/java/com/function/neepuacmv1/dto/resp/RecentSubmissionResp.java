package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 最近提交（用于题目页右侧 Recent Submissions） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentSubmissionResp {
    private Long submissionId;
    private String status;
    private String language;
    private Integer timeMs;
    private Integer memoryKb;
    private Integer codeLength;
    private LocalDateTime createdAt;
}
