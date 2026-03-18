package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminContestUpdateReq {
    private Long id;

    private String title;
    private String mode;
    private Integer showInList;
    private String visibility;
    private String password; // 传空字符串表示清空密码（仅 protected/private）

    private LocalDateTime startAt;
    private Integer durationMinutes;

    private String sourceInfo;
    private String announcement;
    private Integer freezeMinutes;

    // 赛时可见性
    private Integer duringOtherSubVisible;
    private Integer duringStandingsVisible;
    private Integer duringPercentageVisible;
    private Integer duringTestcaseScoreVisible;
    // 赛后可见性
    private Integer afterOtherSubVisible;
    private Integer afterStandingsVisible;
    private Integer afterPercentageVisible;
    private Integer afterTestcaseScoreVisible;
}
