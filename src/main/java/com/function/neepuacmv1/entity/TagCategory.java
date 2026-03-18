package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 标签大类：acm_tag_category */
@Data
@TableName("acm_tag_category")
public class TagCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
