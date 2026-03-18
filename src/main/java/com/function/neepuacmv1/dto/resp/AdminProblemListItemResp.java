package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 管理端题库列表行 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProblemListItemResp {
    private Long id;
    private String problemCode;
    private String title;
    private Integer isPublic;
    private Long judgeTemplateId;
    private Integer statementCount;
    private Integer acceptedCount;
    private Integer submittedCount;
    private LocalDateTime updatedAt;
}
