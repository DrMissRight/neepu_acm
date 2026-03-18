package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionListItemResp {
    private Long id;

    private Long userId;
    private String username;     // join user
    private Long problemId;
    private String problemCode;  // join problem
    private String title;        // join problem

    private String status;
    private String language;

    private Integer timeMs;
    private Integer memoryKb;
    private Integer codeLength;

    private LocalDateTime createdAt;
}
