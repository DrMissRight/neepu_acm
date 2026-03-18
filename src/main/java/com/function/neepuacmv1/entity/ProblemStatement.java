package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多版本题面：acm_problem_statement
 */
@Data
@TableName("acm_problem_statement")
public class ProblemStatement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    /**
     * zh-CN / en / ...
     */
    private String lang;

    @TableField("version_name")
    private String versionName;

    @TableField("content_md")
    private String contentMd;

    /**
     * 列表可编辑标题
     */
    private String title;

    @TableField("is_public")
    private Integer isPublic;

    @TableField("is_default")
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
