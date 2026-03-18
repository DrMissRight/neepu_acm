package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContestListItemResp {
    private Long id;
    private String title;
    private String mode;
    private String visibility;
    private Integer showInList;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer freezeMinutes;
    private LocalDateTime updatedAt;
}
