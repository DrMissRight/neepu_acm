package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_submission")
public class Submission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("problem_id")
    private Long problemId;

    @TableField("contest_id")
    private Long contestId;

    private String language;

    private String code;

    @TableField("code_length")
    private Integer codeLength;

    private String status;

    @TableField("time_ms")
    private Integer timeMs;

    @TableField("memory_kb")
    private Integer memoryKb;

    private Integer score;

    @TableField("error_msg")
    private String errorMsg;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
