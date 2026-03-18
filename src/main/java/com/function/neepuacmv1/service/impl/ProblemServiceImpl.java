package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.resp.*;
import com.function.neepuacmv1.entity.*;
import com.function.neepuacmv1.mapper.*;
import com.function.neepuacmv1.security.UserContext;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ProblemServiceImpl implements com.function.neepuacmv1.service.ProblemService {

    private final ProblemMapper problemMapper;
    private final ProblemStatementMapper statementMapper;
    private final ProblemTagMapper problemTagMapper;
    private final SubmissionQueryMapper submissionQueryMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper om = new ObjectMapper();

    public ProblemServiceImpl(ProblemMapper problemMapper,
                              ProblemStatementMapper statementMapper,
                              ProblemTagMapper problemTagMapper,
                              SubmissionQueryMapper submissionQueryMapper,
                              RedisUtil redisUtil) {
        this.problemMapper = problemMapper;
        this.statementMapper = statementMapper;
        this.problemTagMapper = problemTagMapper;
        this.submissionQueryMapper = submissionQueryMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result pageProblems(int page, int size, String keyword, String sort, Long tagId) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");

            // sort: "ac" (通过数) / "id"
            String sortKey = (sort == null ? "ac" : sort.trim());

            // 缓存 key（仅对匿名/公共列表缓存，避免把 solved 结果缓存给不同用户）
            Long userId = UserContext.getUserId(); // 若接口对游客开放，userId 可能为 null
            boolean canCache = (userId == null);

            String cacheKey = RedisKeys.PROBLEM_LIST + page + ":" + size + ":" +
                    (keyword == null ? "" : keyword) + ":" + sortKey + ":" + (tagId == null ? 0 : tagId);

            if (canCache) {
                Object cached = redisUtil.get(cacheKey);
                if (cached != null) {
                    @SuppressWarnings("unchecked")
                    List<ProblemListItemResp> list = om.readValue(String.valueOf(cached),
                            om.getTypeFactory().constructCollectionType(List.class, ProblemListItemResp.class));
                    // total 无法从 list 得到，这里不使用缓存 total；要缓存 total 可以改为包装对象
                    return Result.ok(list, (long) list.size());
                }
            }

            LambdaQueryWrapper<Problem> qw = new LambdaQueryWrapper<>();
            qw.eq(Problem::getDeleted, 0).eq(Problem::getIsPublic, 1);

            if (!ValidationUtil.isBlank(keyword)) {
                qw.and(w -> w.like(Problem::getTitle, keyword)
                        .or().like(Problem::getProblemCode, keyword));
            }

            if ("id".equalsIgnoreCase(sortKey)) {
                qw.orderByAsc(Problem::getId);
            } else {
                // 默认按通过数排序
                qw.orderByDesc(Problem::getAcceptedCount).orderByAsc(Problem::getId);
            }

            // tagId 过滤（JOIN 性能更好；这里用二次过滤，数据量大时建议改自定义 SQL）
            Page<Problem> p = problemMapper.selectPage(new Page<>(page, size), qw);
            List<Problem> records = p.getRecords();
            if (records == null) records = Collections.emptyList();

            if (tagId != null && tagId > 0) {
                // 仅保留包含该 tag 的题目
                List<Long> keep = new ArrayList<>();
                for (Problem pr : records) {
                    Long cnt = problemTagMapper.selectCount(new LambdaQueryWrapper<ProblemTag>()
                            .eq(ProblemTag::getProblemId, pr.getId())
                            .eq(ProblemTag::getTagId, tagId));
                    if (cnt != null && cnt > 0) keep.add(pr.getId());
                }
                List<Problem> filtered = records.stream().filter(x -> keep.contains(x.getId())).toList();
                records = filtered;
            }

            // solved（仅登录用户计算）
            List<ProblemListItemResp> resp = new ArrayList<>();
            for (Problem pr : records) {
                Boolean solved = null;
                if (userId != null) {
                    solved = submissionQueryMapper.countAccepted(userId, pr.getId()) > 0;
                }
                resp.add(new ProblemListItemResp(
                        pr.getId(),
                        pr.getProblemCode(),
                        pr.getTitle(),
                        pr.getAcceptedCount(),
                        pr.getSubmittedCount(),
                        solved
                ));
            }

            if (canCache) {
                // 缓存 60 秒：题库列表更新频繁时 TTL 不宜过长
                redisUtil.set(cacheKey, om.writeValueAsString(resp), 60);
            }
            return Result.ok(resp, p.getTotal());
        } catch (Exception e) {
            return Result.fail("获取题库列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result getProblemDetail(Long problemId, Integer recentLimit) {
        try {
            if (problemId == null) return Result.fail("problemId不能为空");
            int limit = (recentLimit == null || recentLimit <= 0 || recentLimit > 50) ? 5 : recentLimit;

            Long userId = UserContext.getUserId();

            // 题目详情缓存：只缓存“公共不含用户信息部分”
            String cacheKey = RedisKeys.PROBLEM_DETAIL + problemId;
            ProblemDetailResp base;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                base = om.readValue(String.valueOf(cached), ProblemDetailResp.class);
            } else {
                Problem pr = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId)
                        .eq(Problem::getDeleted, 0)
                        .eq(Problem::getIsPublic, 1));
                if (pr == null) return Result.fail("题目不存在或不可见");

                List<ProblemStatement> statements = statementMapper.selectList(new LambdaQueryWrapper<ProblemStatement>()
                        .eq(ProblemStatement::getProblemId, problemId)
                        .orderByDesc(ProblemStatement::getIsDefault)
                        .orderByAsc(ProblemStatement::getId));

                List<ProblemStatementResp> descs = new ArrayList<>();
                ProblemStatementResp defaultDesc = null;
                if (statements != null) {
                    for (ProblemStatement st : statements) {
                        ProblemStatementResp one = new ProblemStatementResp(
                                st.getId(),
                                st.getLang(),
                                st.getVersionName(),
                                st.getContentMd(),
                                st.getIsDefault() != null && st.getIsDefault() == 1
                        );
                        descs.add(one);
                        if (defaultDesc == null && one.getIsDefault()) defaultDesc = one;
                    }
                }
                if (defaultDesc == null && !descs.isEmpty()) defaultDesc = descs.get(0);

                List<String> tags = problemTagMapper.listTagNamesByProblemId(problemId);
                if (tags == null) tags = Collections.emptyList();

                base = new ProblemDetailResp(
                        pr.getId(),
                        pr.getProblemCode(),
                        pr.getTitle(),
                        pr.getTimeLimitMs(),
                        pr.getMemoryLimitKb(),
                        pr.getDifficulty(),
                        pr.getSource(),
                        pr.getAcceptedCount(),
                        pr.getSubmittedCount(),
                        defaultDesc,
                        descs,
                        tags,
                        Collections.emptyList(), // 用户相关：后面填
                        null // solved：后面填
                );

                // 缓存 5 分钟（题面不经常变；管理端修改题面时建议删除该 key）
                redisUtil.set(cacheKey, om.writeValueAsString(base), 300);
            }

            // 补充用户相关：recent submissions + solved
            List<RecentSubmissionResp> recent = new ArrayList<>();
            Boolean solved = null;

            if (userId != null) {
                solved = submissionQueryMapper.countAccepted(userId, problemId) > 0;
                List<Map<String, Object>> rows = submissionQueryMapper.listRecentByUserAndProblem(userId, problemId, limit);
                if (rows != null) {
                    for (Map<String, Object> r : rows) {
                        RecentSubmissionResp rs = new RecentSubmissionResp(
                                toLong(r.get("submissionId")),
                                str(r.get("status")),
                                str(r.get("language")),
                                toInt(r.get("timeMs")),
                                toInt(r.get("memoryKb")),
                                toInt(r.get("codeLength")),
                                (java.time.LocalDateTime) r.get("createdAt")
                        );
                        recent.add(rs);
                    }
                }
            }

            // 组装最终返回（不改缓存 base）
            ProblemDetailResp resp = new ProblemDetailResp(
                    base.getId(),
                    base.getProblemCode(),
                    base.getTitle(),
                    base.getTimeLimitMs(),
                    base.getMemoryLimitKb(),
                    base.getDifficulty(),
                    base.getSource(),
                    base.getAcceptedCount(),
                    base.getSubmittedCount(),
                    base.getDescription(),
                    base.getDescriptions(),
                    base.getTags(),
                    recent,
                    solved
            );

            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取题目详情失败：" + e.getMessage());
        }
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static Integer toInt(Object o) {
        if (o == null) return null;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private static Long toLong(Object o) {
        if (o == null) return null;
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}
