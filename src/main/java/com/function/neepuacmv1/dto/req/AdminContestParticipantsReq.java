package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class AdminContestParticipantsReq {
    private Long contestId;
    /** 官方参赛者 userId 列表 */
    private List<Long> participants;
    /** 非官方参赛者 userId 列表 */
    private List<Long> unofficialParticipants;
}
