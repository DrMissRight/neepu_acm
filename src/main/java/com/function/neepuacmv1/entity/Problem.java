package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目基础信息：acm_problem
 */
@Data
@TableName("acm_problem")
public class Problem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("problem_code")
    private String problemCode;

    private String title;

    @TableField("time_limit_ms")
    private Integer timeLimitMs;

    @TableField("memory_limit_kb")
    private Integer memoryLimitKb;

    private Integer difficulty;

    private String source;

    @TableField("accepted_count")
    private Integer acceptedCount;

    @TableField("submitted_count")
    private Integer submittedCount;

    @TableField("is_public")
    private Integer isPublic;

    /**
     * 选定评测模板
     */
    @TableField("judge_template_id")
    private Long judgeTemplateId;

    @TableField("statement_count")
    private Integer statementCount;


    private Integer deleted;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
