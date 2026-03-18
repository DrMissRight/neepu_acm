package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_contest_problem")
public class ContestProblem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("contest_id")
    private Long contestId;

    @TableField("problem_id")
    private Long problemId;

    @TableField("problem_code")
    private String problemCode;

    private String alias;
    private Integer weight;

    @TableField("balloon_color")
    private String balloonColor;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("statement_mode")
    private String statementMode; // BASE/CUSTOM

    @TableField("base_statement_id")
    private Long baseStatementId;

    @TableField("custom_statement_id")
    private Long customStatementId;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
