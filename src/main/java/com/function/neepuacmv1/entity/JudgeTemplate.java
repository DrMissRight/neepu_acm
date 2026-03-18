package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 评测模板：acm_judge_template */
@Data
@TableName("acm_judge_template")
public class JudgeTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    /** IO / SPJ / Advanced */
    private String type;

    /** 语言标识（IO 常用：cpp11/python3/java8 等），SPJ/Advanced可空 */
    private String lang;

    @TableField("script_json")
    private String scriptJson;

    private String description;

    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("is_public")
    private Integer isPublic;

    @TableField("fork_from_id")
    private Long forkFromId;

    @TableField("created_by")
    private Long createdBy;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
