package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailResp {
    private Long id;

    private Long userId;
    private String username;

    private Long problemId;
    private String problemCode;
    private String title;

    private String status;
    private String language;

    private Integer timeMs;
    private Integer memoryKb;
    private Integer score;
    private Integer codeLength;

    private String errorMsg;

    /** 仅本人/管理员可见 */
    private String code;

    /** 每个测试点结果（可选） */
    private List<SubmissionCaseResp> cases;

    private LocalDateTime createdAt;
}
