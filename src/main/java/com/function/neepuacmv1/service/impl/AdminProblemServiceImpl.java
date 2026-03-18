package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminProblemCreateReq;
import com.function.neepuacmv1.dto.req.AdminProblemUpdateReq;
import com.function.neepuacmv1.dto.resp.AdminProblemDetailResp;
import com.function.neepuacmv1.dto.resp.AdminProblemListItemResp;
import com.function.neepuacmv1.entity.Problem;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.mapper.AdminProblemQueryMapper;
import com.function.neepuacmv1.mapper.ProblemMapper;
import com.function.neepuacmv1.service.AdminProblemService;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AdminProblemServiceImpl implements AdminProblemService {

    private final ProblemMapper problemMapper;
    private final AdminProblemQueryMapper queryMapper;
    private final RedisUtil redisUtil;

    public AdminProblemServiceImpl(ProblemMapper problemMapper, AdminProblemQueryMapper queryMapper, RedisUtil redisUtil) {
        this.problemMapper = problemMapper;
        this.queryMapper = queryMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result page(int page, int size, String keyword) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");
            int offset = (page - 1) * size;
            long total = queryMapper.countProblems(keyword);
            List<Map<String,Object>> rows = queryMapper.pageProblems(keyword, offset, size);

            List<AdminProblemListItemResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminProblemListItemResp(
                        toLong(r.get("id")),
                        str(r.get("problem_code")),
                        str(r.get("title")),
                        toInt(r.get("is_public")),
                        toLong(r.get("judge_template_id")),
                        toInt(r.get("statement_count")),
                        toInt(r.get("accepted_count")),
                        toInt(r.get("submitted_count")),
                        (java.time.LocalDateTime) r.get("updated_at")
                ));
            }
            return Result.ok(list, total);
        } catch (Exception e) {
            return Result.fail("获取题库列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result detail(Long problemId) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, problemId).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            AdminProblemDetailResp resp = new AdminProblemDetailResp(
                    p.getId(), p.getProblemCode(), p.getTitle(),
                    p.getTimeLimitMs(), p.getMemoryLimitKb(),
                    p.getDifficulty(), p.getSource(),
                    p.getIsPublic(), p.getJudgeTemplateId()
            );
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取题目详情失败：" + e.getMessage());
        }
    }

    @Override
    public Result create(AdminProblemCreateReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getProblemCode())) return Result.fail("题号不能为空");
            if (ValidationUtil.isBlank(req.getTitle())) return Result.fail("标题不能为空");

            // 题号唯一
            Long cnt = problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getProblemCode, req.getProblemCode())
                    .eq(Problem::getDeleted, 0));
            if (cnt != null && cnt > 0) return Result.fail("题号已存在");
            Problem p = new Problem();
            p.setProblemCode(req.getProblemCode().trim());
            p.setTitle(req.getTitle().trim());
            p.setTimeLimitMs(req.getTimeLimitMs() == null ? 1000 : req.getTimeLimitMs());
            p.setMemoryLimitKb(req.getMemoryLimitKb() == null ? 262144 : req.getMemoryLimitKb());
            p.setDifficulty(req.getDifficulty());
            p.setSource(req.getSource());
            p.setIsPublic(req.getIsPublic() == null ? 1 : req.getIsPublic());
            p.setJudgeTemplateId(req.getJudgeTemplateId());
            p.setAcceptedCount(0);
            p.setSubmittedCount(0);
            p.setStatementCount(0);
            p.setDeleted(0);

            int rows = problemMapper.insert(p);
            if (rows <= 0) return Result.fail("新增题目失败");

            // 失效前台缓存：列表（可以简单删前缀，或你实现里是按 key hash）
            // 最小策略：不全删，等 TTL；但题库管理建议立即刷新前台
            // 这里给最小可用：删详情 key（新题没有）+ 可选清理 list 前缀（需要 scan）
            return Result.ok(Collections.singletonMap("problemId", p.getId()));
        } catch (Exception e) {
            return Result.fail("新增题目失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(AdminProblemUpdateReq req) {
        try {
            if (req == null || req.getId() == null) return Result.fail("id不能为空");

            Problem exist = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, req.getId()).eq(Problem::getDeleted, 0));
            if (exist == null) return Result.fail("题目不存在");

            Problem upd = new Problem();
            upd.setId(req.getId());
            if (!ValidationUtil.isBlank(req.getTitle())) upd.setTitle(req.getTitle().trim());
            if (req.getTimeLimitMs() != null) upd.setTimeLimitMs(req.getTimeLimitMs());
            if (req.getMemoryLimitKb() != null) upd.setMemoryLimitKb(req.getMemoryLimitKb());
            if (req.getDifficulty() != null) upd.setDifficulty(req.getDifficulty());
            if (req.getSource() != null) upd.setSource(req.getSource());
            if (req.getIsPublic() != null) upd.setIsPublic(req.getIsPublic());
            if (req.getJudgeTemplateId() != null) upd.setJudgeTemplateId(req.getJudgeTemplateId());

            int rows = problemMapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            // 失效前台详情缓存
            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + req.getId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新题目失败：" + e.getMessage());
        }
    }

    @Override
    public Result deleteBatch(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");

            for (Long id : req.getIds()) {
                if (id == null) continue;
                int rows = problemMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Problem>()
                                .eq(Problem::getId, id)
                                .eq(Problem::getDeleted, 0)
                                .set(Problem::getDeleted, 1)
                                .set(Problem::getIsPublic, 0)
                );
                if (rows > 0) {
                    redisUtil.delete(RedisKeys.PROBLEM_DETAIL + id);
                }
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除题目失败：" + e.getMessage());
        }
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}
