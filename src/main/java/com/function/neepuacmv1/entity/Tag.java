package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 子标签：acm_tag */
@Data
@TableName("acm_tag")
public class Tag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    private String name;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
