package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("acm_contest")
public class Contest {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;
    private String mode;

    @TableField("show_in_list")
    private Integer showInList;

    private String visibility;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("start_at")
    private LocalDateTime startAt;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("end_at")
    private LocalDateTime endAt;

    @TableField("source_info")
    private String sourceInfo;

    private String announcement;

    @TableField("freeze_minutes")
    private Integer freezeMinutes;

    @TableField("during_other_sub_visible")
    private Integer duringOtherSubVisible;
    @TableField("during_standings_visible")
    private Integer duringStandingsVisible;
    @TableField("during_percentage_visible")
    private Integer duringPercentageVisible;
    @TableField("during_testcase_score_visible")
    private Integer duringTestcaseScoreVisible;

    @TableField("after_other_sub_visible")
    private Integer afterOtherSubVisible;
    @TableField("after_standings_visible")
    private Integer afterStandingsVisible;
    @TableField("after_percentage_visible")
    private Integer afterPercentageVisible;
    @TableField("after_testcase_score_visible")
    private Integer afterTestcaseScoreVisible;

    private Integer deleted;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
