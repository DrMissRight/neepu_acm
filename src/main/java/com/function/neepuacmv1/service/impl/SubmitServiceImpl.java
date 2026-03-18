package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.function.neepuacmv1.constant.JudgeQueueConstants;
import com.function.neepuacmv1.constant.SubmissionStatus;
import com.function.neepuacmv1.dto.req.SubmitReq;
import com.function.neepuacmv1.entity.Problem;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.Submission;
import com.function.neepuacmv1.mapper.ProblemMapper;
import com.function.neepuacmv1.mapper.SubmissionMapper;
import com.function.neepuacmv1.service.SubmitService;
import com.function.neepuacmv1.utils.UserContext;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class SubmitServiceImpl implements SubmitService {

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public SubmitServiceImpl(SubmissionMapper submissionMapper,
                             ProblemMapper problemMapper,
                             StringRedisTemplate stringRedisTemplate) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public Result submit(SubmitReq req) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) return Result.fail("未登录");

            if (req == null) return Result.fail("请求参数不能为空");
            if (req.getProblemId() == null) return Result.fail("problemId不能为空");
            if (ValidationUtil.isBlank(req.getLanguage())) return Result.fail("language不能为空");
            if (ValidationUtil.isBlank(req.getCode())) return Result.fail("code不能为空");

            if (req.getCode().length() > 200_000) return Result.fail("代码过长");

            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, req.getProblemId())
                    .eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            Submission s = new Submission();
            s.setUserId(userId);
            s.setProblemId(req.getProblemId());
            s.setContestId(req.getContestId());
            s.setLanguage(req.getLanguage().trim());
            s.setCode(req.getCode());
            s.setCodeLength(req.getCode().length());
            s.setStatus(SubmissionStatus.PENDING);
            s.setDeleted(0);

            int ins = submissionMapper.insert(s);
            if (ins <= 0) return Result.fail("提交失败");

            // XADD 入队
            Map<String, String> body = new HashMap<>();
            body.put("submissionId", String.valueOf(s.getId()));
            body.put("problemId", String.valueOf(req.getProblemId()));
            body.put("language", s.getLanguage());

            RecordId rid = stringRedisTemplate.opsForStream().add(
                    StreamRecords.mapBacked(body).withStreamKey(JudgeQueueConstants.STREAM_KEY)
            );

            if (rid == null) {
                // 入队失败：标记 SE
                Submission upd = new Submission();
                upd.setId(s.getId());
                upd.setStatus(SubmissionStatus.SE);
                upd.setErrorMsg("入队失败");
                submissionMapper.updateById(upd);
                return Result.fail("提交入队失败");
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("submissionId", s.getId());
            resp.put("queueMsgId", rid.getValue());
            return Result.ok(resp);

        } catch (Exception e) {
            return Result.fail("提交失败：" + e.getMessage());
        }
    }
}
