package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_submission_case")
public class SubmissionCase {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("submission_id")
    private Long submissionId;

    @TableField("testcase_id")
    private Long testcaseId;

    private String status;

    @TableField("time_ms")
    private Integer timeMs;

    @TableField("memory_kb")
    private Integer memoryKb;

    private Integer score;

    private String info;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;
}
