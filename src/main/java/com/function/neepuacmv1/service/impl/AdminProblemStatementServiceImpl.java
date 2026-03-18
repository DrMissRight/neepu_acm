package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.req.AdminBatchIdsReq;
import com.function.neepuacmv1.dto.req.AdminStatementCreateReq;
import com.function.neepuacmv1.dto.req.AdminStatementToggleReq;
import com.function.neepuacmv1.dto.req.AdminStatementUpdateReq;
import com.function.neepuacmv1.dto.resp.AdminStatementListResp;
import com.function.neepuacmv1.entity.Problem;
import com.function.neepuacmv1.entity.ProblemStatement;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.mapper.AdminProblemQueryMapper;
import com.function.neepuacmv1.mapper.ProblemMapper;
import com.function.neepuacmv1.mapper.ProblemStatementMapper;
import com.function.neepuacmv1.service.AdminProblemStatementService;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AdminProblemStatementServiceImpl implements AdminProblemStatementService {

    private final ProblemStatementMapper statementMapper;
    private final ProblemMapper problemMapper;
    private final AdminProblemQueryMapper queryMapper;
    private final RedisUtil redisUtil;

    public AdminProblemStatementServiceImpl(ProblemStatementMapper statementMapper,
                                            ProblemMapper problemMapper,
                                            AdminProblemQueryMapper queryMapper,
                                            RedisUtil redisUtil) {
        this.statementMapper = statementMapper;
        this.problemMapper = problemMapper;
        this.queryMapper = queryMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result listByProblem(Long problemId) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, problemId).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            List<Map<String,Object>> rows = queryMapper.listStatements(problemId);
            List<AdminStatementListResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminStatementListResp(
                        toLong(r.get("id")),
                        toLong(r.get("problem_id")),
                        str(r.get("lang")),
                        str(r.get("version_name")),
                        str(r.get("title")),
                        toInt(r.get("is_default")),
                        toInt(r.get("is_public")),
                        (java.time.LocalDateTime) r.get("updated_at")
                ));
            }
            return Result.ok(list);
        } catch (Exception e) {
            return Result.fail("获取题面列表失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result create(AdminStatementCreateReq req) {
        try {
            if (req == null || req.getProblemId() == null) return Result.fail("problemId不能为空");
            if (ValidationUtil.isBlank(req.getLang())) return Result.fail("lang不能为空");
            if (ValidationUtil.isBlank(req.getVersionName())) return Result.fail("versionName不能为空");

            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, req.getProblemId()).eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目不存在");

            ProblemStatement st = new ProblemStatement();
            st.setProblemId(req.getProblemId());
            st.setLang(req.getLang().trim());
            st.setVersionName(req.getVersionName().trim());
            st.setTitle(ValidationUtil.isBlank(req.getTitle()) ? st.getVersionName() : req.getTitle().trim());
            st.setContentMd(req.getContentMd() == null ? "" : req.getContentMd());
            st.setIsPublic(req.getIsPublic() == null ? 1 : req.getIsPublic());
            st.setIsDefault(req.getIsDefault() == null ? 0 : req.getIsDefault());

            // 若设为默认：先把其他默认置 0
            if (st.getIsDefault() == 1) {
                statementMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProblemStatement>()
                                .eq(ProblemStatement::getProblemId, req.getProblemId())
                                .set(ProblemStatement::getIsDefault, 0));
            }

            int rows = statementMapper.insert(st);
            if (rows <= 0) return Result.fail("新增题面失败");

            // 更新计数
            problemMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Problem>()
                            .eq(Problem::getId, req.getProblemId())
                            .set(Problem::getStatementCount, (p.getStatementCount() == null ? 0 : p.getStatementCount()) + 1));

            // 失效前台详情缓存
            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + req.getProblemId());
            return Result.ok(Collections.singletonMap("statementId", st.getId()));
        } catch (Exception e) {
            return Result.fail("新增题面失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(AdminStatementUpdateReq req) {
        try {
            if (req == null || req.getId() == null) return Result.fail("id不能为空");

            ProblemStatement st = statementMapper.selectById(req.getId());
            if (st == null) return Result.fail("题面不存在");

            ProblemStatement upd = new ProblemStatement();
            upd.setId(req.getId());
            if (!ValidationUtil.isBlank(req.getTitle())) upd.setTitle(req.getTitle().trim());
            if (req.getContentMd() != null) upd.setContentMd(req.getContentMd());

            int rows = statementMapper.updateById(upd);
            if (rows <= 0) return Result.fail("保存失败");

            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + st.getProblemId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("保存题面失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result toggle(AdminStatementToggleReq req) {
        try {
            if (req == null || req.getId() == null) return Result.fail("id不能为空");

            ProblemStatement st = statementMapper.selectById(req.getId());
            if (st == null) return Result.fail("题面不存在");

            // 默认题面开关：打开则把同题其他设为 0
            if (req.getIsDefault() != null && req.getIsDefault() == 1) {
                statementMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProblemStatement>()
                                .eq(ProblemStatement::getProblemId, st.getProblemId())
                                .set(ProblemStatement::getIsDefault, 0));
            }

            ProblemStatement upd = new ProblemStatement();
            upd.setId(req.getId());
            if (req.getIsDefault() != null) upd.setIsDefault(req.getIsDefault());
            if (req.getIsPublic() != null) upd.setIsPublic(req.getIsPublic());

            int rows = statementMapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            redisUtil.delete(RedisKeys.PROBLEM_DETAIL + st.getProblemId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新题面失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result deleteBatch(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");

            // 查出涉及的 problemId，用于缓存失效和计数修正
            List<ProblemStatement> sts = statementMapper.selectList(new LambdaQueryWrapper<ProblemStatement>()
                    .in(ProblemStatement::getId, req.getIds()));
            if (sts == null || sts.isEmpty()) return Result.ok();

            Set<Long> problemIds = new HashSet<>();
            for (ProblemStatement st : sts) problemIds.add(st.getProblemId());

            // 直接物理删除（题面通常允许删除）；若你希望软删可加 deleted 字段
            int rows = statementMapper.deleteBatchIds(req.getIds());
            if (rows <= 0) return Result.fail("删除失败");

            // 计数修正：粗略做法重新 count
            for (Long pid : problemIds) {
                Long c = statementMapper.selectCount(new LambdaQueryWrapper<ProblemStatement>()
                        .eq(ProblemStatement::getProblemId, pid));
                problemMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Problem>()
                                .eq(Problem::getId, pid)
                                .set(Problem::getStatementCount, c == null ? 0 : c.intValue()));
                redisUtil.delete(RedisKeys.PROBLEM_DETAIL + pid);
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除题面失败：" + e.getMessage());
        }
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}
