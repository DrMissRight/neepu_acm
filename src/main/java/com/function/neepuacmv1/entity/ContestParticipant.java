package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_contest_participant")
public class ContestParticipant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("contest_id")
    private Long contestId;

    @TableField("user_id")
    private Long userId;

    @TableField("is_official")
    private Integer isOfficial;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;
}
