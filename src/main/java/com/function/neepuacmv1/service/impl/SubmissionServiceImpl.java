package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.constant.SubmissionStatus;
import com.function.neepuacmv1.dto.req.SubmissionQueryReq;
import com.function.neepuacmv1.dto.resp.*;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.SubmissionCase;
import com.function.neepuacmv1.mapper.SubmissionCaseMapper;
import com.function.neepuacmv1.mapper.SubmissionQueryMapper;
import com.function.neepuacmv1.service.SubmissionService;
import com.function.neepuacmv1.utils.JsonUtil;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.UserContext;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionQueryMapper queryMapper;
    private final SubmissionCaseMapper caseMapper; // 可选（若你没建 case 表，可移除相关逻辑）
    private final RedisUtil redisUtil;

    private static final long DETAIL_TTL_SECONDS = 120;

    public SubmissionServiceImpl(SubmissionQueryMapper queryMapper,
                                 SubmissionCaseMapper caseMapper,
                                 RedisUtil redisUtil) {
        this.queryMapper = queryMapper;
        this.caseMapper = caseMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result page(SubmissionQueryReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            int page = req.getPage() == null ? 1 : req.getPage();
            int size = req.getSize() == null ? 20 : req.getSize();
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");

            String status = normalizeStatus(req.getStatus());
            if (req.getStatus() != null && status == null) return Result.fail("status不合法");

            String sortBy = normalizeSort(req.getSortBy());
            String order = normalizeOrder(req.getOrder());

            int offset = (page - 1) * size;

            long total = queryMapper.count(req.getUser(), req.getProblem(), status, req.getLanguage());
            List<Map<String,Object>> rows = queryMapper.page(req.getUser(), req.getProblem(), status, req.getLanguage(),
                    sortBy, order, offset, size);

            List<SubmissionListItemResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new SubmissionListItemResp(
                        toLong(r.get("id")),
                        toLong(r.get("user_id")),
                        str(r.get("username")),
                        toLong(r.get("problem_id")),
                        str(r.get("problem_code")),
                        str(r.get("title")),
                        str(r.get("status")),
                        str(r.get("language")),
                        toInt(r.get("time_ms")),
                        toInt(r.get("memory_kb")),
                        toInt(r.get("code_length")),
                        (java.time.LocalDateTime) r.get("created_at")
                ));
            }
            return Result.ok(list, total);
        } catch (Exception e) {
            return Result.fail("获取提交记录失败：" + e.getMessage());
        }
    }

    @Override
    public Result detail(Long id) {
        try {
            if (id == null) return Result.fail("id不能为空");

            // detail 缓存（短TTL）
            String cacheKey = RedisKeys.SUBMISSION_DETAIL + id;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null && !String.valueOf(cached).isBlank()) {
                SubmissionDetailResp resp = JsonUtil.fromJson(String.valueOf(cached), SubmissionDetailResp.class);
                // 仍需按权限处理 code（缓存里可能含 code）
                maskCodeIfNeeded(resp);
                return Result.ok(resp);
            }

            Map<String,Object> r = queryMapper.detail(id);
            if (r == null || r.isEmpty()) return Result.fail("提交记录不存在");

            SubmissionDetailResp resp = new SubmissionDetailResp(
                    toLong(r.get("id")),
                    toLong(r.get("user_id")),
                    str(r.get("username")),
                    toLong(r.get("problem_id")),
                    str(r.get("problem_code")),
                    str(r.get("title")),
                    str(r.get("status")),
                    str(r.get("language")),
                    toInt(r.get("time_ms")),
                    toInt(r.get("memory_kb")),
                    toInt(r.get("score")),
                    toInt(r.get("code_length")),
                    str(r.get("error_msg")),
                    str(r.get("code")),
                    null,
                    (java.time.LocalDateTime) r.get("created_at")
            );

            // case 结果（可选）
            List<SubmissionCase> cases = caseMapper.selectList(new LambdaQueryWrapper<SubmissionCase>()
                    .eq(SubmissionCase::getSubmissionId, id)
                    .orderByAsc(SubmissionCase::getTestcaseId));
            if (cases != null && !cases.isEmpty()) {
                List<SubmissionCaseResp> caseResps = new ArrayList<>();
                for (SubmissionCase c : cases) {
                    caseResps.add(new SubmissionCaseResp(
                            c.getTestcaseId(), c.getStatus(), c.getTimeMs(), c.getMemoryKb(), c.getScore(), c.getInfo()
                    ));
                }
                resp.setCases(caseResps);
            }

            // 写缓存（写入完整 resp；返回前再按权限脱敏）
            redisUtil.set(cacheKey, JsonUtil.toJson(resp), DETAIL_TTL_SECONDS);
            maskCodeIfNeeded(resp);

            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取提交详情失败：" + e.getMessage());
        }
    }

    @Override
    public Result recentByProblem(Long problemId, Integer limit) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            int lim = (limit == null || limit <= 0 || limit > 50) ? 5 : limit;

            List<Map<String,Object>> rows = queryMapper.recentByProblem(problemId, lim);
            List<Map<String,Object>> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", toLong(r.get("id")));
                m.put("userId", toLong(r.get("user_id")));
                m.put("status", str(r.get("status")));
                m.put("language", str(r.get("language")));
                m.put("timeMs", toInt(r.get("time_ms")));
                m.put("memoryKb", toInt(r.get("memory_kb")));
                m.put("codeLength", toInt(r.get("code_length")));
                m.put("createdAt", r.get("created_at"));
                list.add(m);
            }
            return Result.ok(list);
        } catch (Exception e) {
            return Result.fail("获取最近提交失败：" + e.getMessage());
        }
    }

    // -------- helpers --------

    private void maskCodeIfNeeded(SubmissionDetailResp resp) {
        Long me = UserContext.getUserId();
        boolean adminLike = UserContext.isAdminLike();
        boolean isOwner = (me != null && resp.getUserId() != null && me.equals(resp.getUserId()));
        if (!adminLike && !isOwner) {
            resp.setCode(null);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase(Locale.ROOT);
        return SubmissionStatus.ALL.contains(s) ? s : null;
    }

    private String normalizeSort(String sortBy) {
        if (ValidationUtil.isBlank(sortBy)) return "createdAt";
        String s = sortBy.trim().toLowerCase(Locale.ROOT);
        if (s.equals("time")) return "time";
        if (s.equals("memory")) return "memory";
        return "createdAt";
    }

    private String normalizeOrder(String order) {
        if (ValidationUtil.isBlank(order)) return "desc";
        String s = order.trim().toLowerCase(Locale.ROOT);
        return s.equals("asc") ? "asc" : "desc";
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}
