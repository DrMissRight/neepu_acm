package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContestDetailResp {
    private Long id;
    private String title;
    private String mode;
    private Integer showInList;
    private String visibility;

    private LocalDateTime startAt;
    private Integer durationMinutes;
    private LocalDateTime endAt;

    private String sourceInfo;
    private String announcement;

    private Integer freezeMinutes;

    private Integer duringOtherSubVisible;
    private Integer duringStandingsVisible;
    private Integer duringPercentageVisible;
    private Integer duringTestcaseScoreVisible;

    private Integer afterOtherSubVisible;
    private Integer afterStandingsVisible;
    private Integer afterPercentageVisible;
    private Integer afterTestcaseScoreVisible;

    private List<Long> participants;
    private List<Long> unofficialParticipants;
}
