package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminContestCreateReq {
    private String title;
    private String mode;           // OI/IOI/ACM
    private Integer showInList;    // 1/0
    private String visibility;     // public/protected/private
    private String password;       // 仅 protected/private 需要

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
