package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;

public interface AdminContestService {

    Result page(int page, int size, String keyword);

    Result detail(Long contestId);

    Result create(AdminContestCreateReq req);

    Result update(AdminContestUpdateReq req);

    Result deleteBatch(AdminBatchIdsReq req);

    // participants
    Result updateParticipants(AdminContestParticipantsReq req);

    // contest problems
    Result listProblems(Long contestId);

    Result addProblem(AdminContestProblemAddReq req);

    Result updateProblem(AdminContestProblemUpdateReq req);

    Result removeProblem(Long contestId, Long contestProblemId);

    Result validateProblemCode(String problemCode);

    // statement override
    Result getContestProblemStatement(Long contestId, Long contestProblemId);

    Result setContestProblemStatement(AdminContestProblemStatementReq req);
}
