package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_contest_problem_statement")
public class ContestProblemStatement {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("contest_id")
    private Long contestId;

    @TableField("contest_problem_id")
    private Long contestProblemId;

    private String lang;
    private String title;

    @TableField("content_md")
    private String contentMd;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
