package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.dto.resp.*;
import com.function.neepuacmv1.entity.*;
import com.function.neepuacmv1.mapper.*;
import com.function.neepuacmv1.service.AdminContestService;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminContestServiceImpl implements AdminContestService {

    private final ContestMapper contestMapper;
    private final ContestParticipantMapper participantMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestProblemStatementMapper contestStatementMapper;
    private final AdminContestQueryMapper queryMapper;

    private final ProblemMapper problemMapper; // 题库表（你题库模块已有）
    private final ProblemStatementMapper problemStatementMapper; // 题库题面表（你题库模块已有）

    private final RedisUtil redisUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ObjectMapper om = new ObjectMapper();

    public AdminContestServiceImpl(
            ContestMapper contestMapper,
            ContestParticipantMapper participantMapper,
            ContestProblemMapper contestProblemMapper,
            ContestProblemStatementMapper contestStatementMapper,
            AdminContestQueryMapper queryMapper,
            ProblemMapper problemMapper,
            ProblemStatementMapper problemStatementMapper,
            RedisUtil redisUtil
    ) {
        this.contestMapper = contestMapper;
        this.participantMapper = participantMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.contestStatementMapper = contestStatementMapper;
        this.queryMapper = queryMapper;
        this.problemMapper = problemMapper;
        this.problemStatementMapper = problemStatementMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result page(int page, int size, String keyword) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");
            int offset = (page - 1) * size;
            long total = queryMapper.countContests(keyword);
            List<Map<String,Object>> rows = queryMapper.pageContests(keyword, offset, size);

            List<AdminContestListItemResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminContestListItemResp(
                        toLong(r.get("id")),
                        str(r.get("title")),
                        str(r.get("mode")),
                        str(r.get("visibility")),
                        toInt(r.get("show_in_list")),
                        (LocalDateTime) r.get("start_at"),
                        (LocalDateTime) r.get("end_at"),
                        toInt(r.get("freeze_minutes")),
                        (LocalDateTime) r.get("updated_at")
                ));
            }
            return Result.ok(list, total);
        } catch (Exception e) {
            return Result.fail("获取比赛列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result detail(Long contestId) {
        try {
            if (contestId == null) return Result.fail("contestId不能为空");

            Contest c = contestMapper.selectOne(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getId, contestId).eq(Contest::getDeleted, 0));
            if (c == null) return Result.fail("比赛不存在");

            List<Long> participants = queryMapper.listParticipants(contestId);
            List<Long> unofficial = queryMapper.listUnofficialParticipants(contestId);

            AdminContestDetailResp resp = new AdminContestDetailResp(
                    c.getId(), c.getTitle(), c.getMode(), c.getShowInList(), c.getVisibility(),
                    c.getStartAt(), c.getDurationMinutes(), c.getEndAt(),
                    c.getSourceInfo(), c.getAnnouncement(),
                    c.getFreezeMinutes(),
                    c.getDuringOtherSubVisible(), c.getDuringStandingsVisible(), c.getDuringPercentageVisible(), c.getDuringTestcaseScoreVisible(),
                    c.getAfterOtherSubVisible(), c.getAfterStandingsVisible(), c.getAfterPercentageVisible(), c.getAfterTestcaseScoreVisible(),
                    participants == null ? Collections.emptyList() : participants,
                    unofficial == null ? Collections.emptyList() : unofficial
            );
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取比赛详情失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result create(AdminContestCreateReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getTitle())) return Result.fail("标题不能为空");
            String mode = nvl(req.getMode(), "ACM").toUpperCase(Locale.ROOT);
            if (!Set.of("OI","IOI","ACM").contains(mode)) return Result.fail("模式只能为 OI/IOI/ACM");

            String visibility = nvl(req.getVisibility(), "public").toLowerCase(Locale.ROOT);
            if (!Set.of("public","protected","private").contains(visibility)) return Result.fail("可见性不合法");

            if (req.getStartAt() == null) return Result.fail("开始时间不能为空");
            if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) return Result.fail("持续时长不合法");

            LocalDateTime endAt = req.getStartAt().plusMinutes(req.getDurationMinutes());
            Integer freeze = req.getFreezeMinutes() == null ? 0 : req.getFreezeMinutes();
            if (freeze < 0 || freeze > req.getDurationMinutes()) return Result.fail("封榜分钟数不合法");

            Contest c = new Contest();
            c.setTitle(req.getTitle().trim());
            c.setMode(mode);
            c.setShowInList(req.getShowInList() == null ? 1 : req.getShowInList());
            c.setVisibility(visibility);

            if (!"public".equals(visibility)) {
                if (ValidationUtil.isBlank(req.getPassword())) return Result.fail("该可见性必须设置密码");
                c.setPasswordHash(encoder.encode(req.getPassword()));
            } else {
                c.setPasswordHash(null);
            }

            c.setStartAt(req.getStartAt());
            c.setDurationMinutes(req.getDurationMinutes());
            c.setEndAt(endAt);

            c.setSourceInfo(req.getSourceInfo());
            c.setAnnouncement(req.getAnnouncement());
            c.setFreezeMinutes(freeze);

            fillVisibilityDefaults(c, req);

            c.setDeleted(0);

            int rows = contestMapper.insert(c);
            if (rows <= 0) return Result.fail("创建比赛失败");

            // 缓存失效
            redisUtil.delete(RedisKeys.CONTEST_LIST);

            return Result.ok(Collections.singletonMap("contestId", c.getId()));
        } catch (Exception e) {
            return Result.fail("创建比赛失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result update(AdminContestUpdateReq req) {
        try {
            if (req == null || req.getId() == null) return Result.fail("id不能为空");
            Contest exist = contestMapper.selectOne(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getId, req.getId()).eq(Contest::getDeleted, 0));
            if (exist == null) return Result.fail("比赛不存在");

            Contest upd = new Contest();
            upd.setId(req.getId());

            if (!ValidationUtil.isBlank(req.getTitle())) upd.setTitle(req.getTitle().trim());

            if (!ValidationUtil.isBlank(req.getMode())) {
                String mode = req.getMode().toUpperCase(Locale.ROOT);
                if (!Set.of("OI","IOI","ACM").contains(mode)) return Result.fail("模式只能为 OI/IOI/ACM");
                upd.setMode(mode);
            }

            if (req.getShowInList() != null) upd.setShowInList(req.getShowInList());

            String newVis = req.getVisibility() == null ? null : req.getVisibility().toLowerCase(Locale.ROOT);
            if (newVis != null) {
                if (!Set.of("public","protected","private").contains(newVis)) return Result.fail("可见性不合法");
                upd.setVisibility(newVis);

                if ("public".equals(newVis)) {
                    upd.setPasswordHash(null);
                } else {
                    // protected/private：允许更新密码；若 password 传空字符串 => 清空（不建议），这里按业务：清空视为失败
                    if (req.getPassword() != null) {
                        if (ValidationUtil.isBlank(req.getPassword())) return Result.fail("密码不能为空");
                        upd.setPasswordHash(encoder.encode(req.getPassword()));
                    }
                }
            } else {
                // visibility 未变，但如果显式传了 password 也允许更新
                if (req.getPassword() != null) {
                    if (ValidationUtil.isBlank(req.getPassword())) return Result.fail("密码不能为空");
                    upd.setPasswordHash(encoder.encode(req.getPassword()));
                }
            }

            LocalDateTime startAt = req.getStartAt() != null ? req.getStartAt() : exist.getStartAt();
            Integer duration = req.getDurationMinutes() != null ? req.getDurationMinutes() : exist.getDurationMinutes();
            if (startAt == null || duration == null || duration <= 0) return Result.fail("时间参数不合法");

            if (req.getStartAt() != null) upd.setStartAt(req.getStartAt());
            if (req.getDurationMinutes() != null) upd.setDurationMinutes(req.getDurationMinutes());

            LocalDateTime endAt = startAt.plusMinutes(duration);
            upd.setEndAt(endAt);

            if (req.getSourceInfo() != null) upd.setSourceInfo(req.getSourceInfo());
            if (req.getAnnouncement() != null) upd.setAnnouncement(req.getAnnouncement());

            if (req.getFreezeMinutes() != null) {
                if (req.getFreezeMinutes() < 0 || req.getFreezeMinutes() > duration) return Result.fail("封榜分钟数不合法");
                upd.setFreezeMinutes(req.getFreezeMinutes());
            }

            // visibility flags（允许部分更新）
            patchVisibility(upd, req);

            int rows = contestMapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            // 缓存失效
            redisUtil.delete(RedisKeys.CONTEST_DETAIL + req.getId());
            redisUtil.delete(RedisKeys.CONTEST_LIST);

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新比赛失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result deleteBatch(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");
            for (Long id : req.getIds()) {
                if (id == null) continue;
                contestMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Contest>()
                                .eq(Contest::getId, id)
                                .eq(Contest::getDeleted, 0)
                                .set(Contest::getDeleted, 1)
                                .set(Contest::getShowInList, 0)
                );
                redisUtil.delete(RedisKeys.CONTEST_DETAIL + id);
                redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + id);
            }
            redisUtil.delete(RedisKeys.CONTEST_LIST);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除比赛失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result updateParticipants(AdminContestParticipantsReq req) {
        try {
            if (req == null || req.getContestId() == null) return Result.fail("contestId不能为空");
            Long contestId = req.getContestId();
            Contest c = contestMapper.selectOne(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getId, contestId).eq(Contest::getDeleted, 0));
            if (c == null) return Result.fail("比赛不存在");

            // 清空旧参与者
            participantMapper.delete(new LambdaQueryWrapper<ContestParticipant>()
                    .eq(ContestParticipant::getContestId, contestId));

            // 插入新参与者
            insertParticipants(contestId, req.getParticipants(), 1);
            insertParticipants(contestId, req.getUnofficialParticipants(), 0);

            redisUtil.delete(RedisKeys.CONTEST_DETAIL + contestId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新参与者失败：" + e.getMessage());
        }
    }

    @Override
    public Result listProblems(Long contestId) {
        try {
            if (contestId == null) return Result.fail("contestId不能为空");
            Contest c = contestMapper.selectOne(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getId, contestId).eq(Contest::getDeleted, 0));
            if (c == null) return Result.fail("比赛不存在");

            List<Map<String,Object>> rows = queryMapper.listContestProblems(contestId);
            List<AdminContestProblemResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminContestProblemResp(
                        toLong(r.get("id")),
                        toLong(r.get("contest_id")),
                        toLong(r.get("problem_id")),
                        str(r.get("problem_code")),
                        str(r.get("alias")),
                        toInt(r.get("weight")),
                        str(r.get("balloon_color")),
                        toInt(r.get("order_index")),
                        str(r.get("statement_mode")),
                        toLong(r.get("base_statement_id")),
                        toLong(r.get("custom_statement_id"))
                ));
            }
            return Result.ok(list);
        } catch (Exception e) {
            return Result.fail("获取比赛题目列表失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result addProblem(AdminContestProblemAddReq req) {
        try {
            if (req == null || req.getContestId() == null) return Result.fail("contestId不能为空");
            if (ValidationUtil.isBlank(req.getProblemCode())) return Result.fail("题目编码不能为空");

            Contest contest = contestMapper.selectOne(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getId, req.getContestId()).eq(Contest::getDeleted, 0));
            if (contest == null) return Result.fail("比赛不存在");

            // 校验题号存在于题库（acm_problem）
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getProblemCode, req.getProblemCode().trim())
                    .eq(Problem::getDeleted, 0));
            if (p == null) return Result.fail("题目编码不合法：题库不存在该题目");

            // alias：若为空则自动生成 A/B/C...
            String alias = req.getAlias();
            if (ValidationUtil.isBlank(alias)) {
                alias = genNextAlias(req.getContestId());
            } else {
                alias = alias.trim().toUpperCase(Locale.ROOT);
            }

            // order_index：取当前最大+1
            Integer maxOrder = contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                            .eq(ContestProblem::getContestId, req.getContestId()))
                    .stream().map(ContestProblem::getOrderIndex).filter(Objects::nonNull)
                    .max(Integer::compareTo).orElse(0);

            ContestProblem cp = new ContestProblem();
            cp.setContestId(req.getContestId());
            cp.setProblemId(p.getId());
            cp.setProblemCode(p.getProblemCode());
            cp.setAlias(alias);
            cp.setWeight(1);
            cp.setBalloonColor(null);
            cp.setOrderIndex(maxOrder + 1);

            // 默认引用题库默认题面（base_statement_id 为空也可，前台渲染时默认取默认题面）
            cp.setStatementMode("BASE");
            cp.setBaseStatementId(null);
            cp.setCustomStatementId(null);

            int rows = contestProblemMapper.insert(cp);
            if (rows <= 0) return Result.fail("添加题目失败");

            redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + req.getContestId());
            return Result.ok(Collections.singletonMap("contestProblemId", cp.getId()));
        } catch (Exception e) {
            return Result.fail("添加比赛题目失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result updateProblem(AdminContestProblemUpdateReq req) {
        try {
            if (req == null || req.getId() == null || req.getContestId() == null) return Result.fail("参数不完整");

            ContestProblem cp = contestProblemMapper.selectById(req.getId());
            if (cp == null || !req.getContestId().equals(cp.getContestId())) return Result.fail("比赛题目不存在");

            ContestProblem upd = new ContestProblem();
            upd.setId(req.getId());

            if (!ValidationUtil.isBlank(req.getAlias())) {
                upd.setAlias(req.getAlias().trim().toUpperCase(Locale.ROOT));
            }
            if (req.getWeight() != null && req.getWeight() <= 0) return Result.fail("权重必须>0");
            if (req.getWeight() != null) upd.setWeight(req.getWeight());
            if (req.getBalloonColor() != null) upd.setBalloonColor(req.getBalloonColor().trim());
            if (req.getOrderIndex() != null) upd.setOrderIndex(req.getOrderIndex());

            if (req.getStatementMode() != null) {
                String sm = req.getStatementMode().trim().toUpperCase(Locale.ROOT);
                if (!Set.of("BASE","CUSTOM").contains(sm)) return Result.fail("statementMode不合法");
                upd.setStatementMode(sm);
                if ("BASE".equals(sm)) {
                    // baseStatementId 可选（null=默认题面）
                    if (req.getBaseStatementId() != null) {
                        // 校验该题面确实属于该题目
                        ProblemStatement st = problemStatementMapper.selectById(req.getBaseStatementId());
                        if (st == null || !Objects.equals(st.getProblemId(), cp.getProblemId())) {
                            return Result.fail("baseStatementId不合法：不属于该题目");
                        }
                        upd.setBaseStatementId(req.getBaseStatementId());
                    } else {
                        upd.setBaseStatementId(null);
                    }
                    // 切回 BASE 不强制删 custom 记录，但 custom_statement_id 可以保留或置空，这里置空更清晰
                    upd.setCustomStatementId(null);
                } else {
                    // CUSTOM：实际内容由 setContestProblemStatement 接口写
                    // 此处仅切模式
                }
            }

            int rows = contestProblemMapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + req.getContestId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新比赛题目失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result removeProblem(Long contestId, Long contestProblemId) {
        try {
            if (contestId == null || contestProblemId == null) return Result.fail("参数不能为空");

            ContestProblem cp = contestProblemMapper.selectById(contestProblemId);
            if (cp == null || !contestId.equals(cp.getContestId())) return Result.fail("比赛题目不存在");

            // 如果有 custom 题面，顺便删
            if (cp.getCustomStatementId() != null) {
                contestStatementMapper.deleteById(cp.getCustomStatementId());
            }

            int rows = contestProblemMapper.deleteById(contestProblemId);
            if (rows <= 0) return Result.fail("删除失败");

            redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + contestId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除比赛题目失败：" + e.getMessage());
        }
    }

    @Override
    public Result validateProblemCode(String problemCode) {
        try {
            if (ValidationUtil.isBlank(problemCode)) return Result.fail("problemCode不能为空");
            Problem p = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getProblemCode, problemCode.trim())
                    .eq(Problem::getDeleted, 0));
            Map<String,Object> resp = new HashMap<>();
            resp.put("valid", p != null);
            resp.put("problemId", p == null ? null : p.getId());
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("校验失败：" + e.getMessage());
        }
    }

    @Override
    public Result getContestProblemStatement(Long contestId, Long contestProblemId) {
        try {
            if (contestId == null || contestProblemId == null) return Result.fail("参数不能为空");
            ContestProblem cp = contestProblemMapper.selectById(contestProblemId);
            if (cp == null || !contestId.equals(cp.getContestId())) return Result.fail("比赛题目不存在");

            if ("CUSTOM".equalsIgnoreCase(cp.getStatementMode()) && cp.getCustomStatementId() != null) {
                ContestProblemStatement st = contestStatementMapper.selectById(cp.getCustomStatementId());
                if (st == null) {
                    return Result.ok(new AdminContestProblemStatementResp(
                            contestId, contestProblemId, "CUSTOM", cp.getBaseStatementId(), null,
                            null, null, null
                    ));
                }
                return Result.ok(new AdminContestProblemStatementResp(
                        contestId, contestProblemId, "CUSTOM", cp.getBaseStatementId(), st.getId(),
                        st.getLang(), st.getTitle(), st.getContentMd()
                ));
            }

            // BASE：不返回题库题面内容（可由前台去题库题面接口拿），这里只返回引用 id
            return Result.ok(new AdminContestProblemStatementResp(
                    contestId, contestProblemId, "BASE", cp.getBaseStatementId(), null,
                    null, null, null
            ));
        } catch (Exception e) {
            return Result.fail("获取题面失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result setContestProblemStatement(AdminContestProblemStatementReq req) {
        try {
            if (req == null || req.getContestId() == null || req.getContestProblemId() == null) return Result.fail("参数不完整");
            ContestProblem cp = contestProblemMapper.selectById(req.getContestProblemId());
            if (cp == null || !req.getContestId().equals(cp.getContestId())) return Result.fail("比赛题目不存在");

            boolean enable = req.getEnableCustom() != null && req.getEnableCustom();

            if (!enable) {
                // 清除自定义题面并切回 BASE
                if (cp.getCustomStatementId() != null) {
                    contestStatementMapper.deleteById(cp.getCustomStatementId());
                }
                ContestProblem upd = new ContestProblem();
                upd.setId(cp.getId());
                upd.setStatementMode("BASE");
                upd.setCustomStatementId(null);
                int rows = contestProblemMapper.updateById(upd);
                if (rows <= 0) return Result.fail("切回BASE失败");
                redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + req.getContestId());
                return Result.ok();
            }

            // enableCustom=true：写入/更新 CUSTOM
            if (ValidationUtil.isBlank(req.getContentMd())) return Result.fail("题面内容不能为空");
            String lang = ValidationUtil.isBlank(req.getLang()) ? "zh-CN" : req.getLang().trim();
            String title = ValidationUtil.isBlank(req.getTitle()) ? nvl(cp.getAlias(), cp.getProblemCode()) : req.getTitle().trim();

            Long customId = cp.getCustomStatementId();
            if (customId == null) {
                ContestProblemStatement st = new ContestProblemStatement();
                st.setContestId(req.getContestId());
                st.setContestProblemId(req.getContestProblemId());
                st.setLang(lang);
                st.setTitle(title);
                st.setContentMd(req.getContentMd());

                int rows = contestStatementMapper.insert(st);
                if (rows <= 0) return Result.fail("保存自定义题面失败");

                ContestProblem upd = new ContestProblem();
                upd.setId(cp.getId());
                upd.setStatementMode("CUSTOM");
                upd.setCustomStatementId(st.getId());
                contestProblemMapper.updateById(upd);
            } else {
                ContestProblemStatement updSt = new ContestProblemStatement();
                updSt.setId(customId);
                updSt.setLang(lang);
                updSt.setTitle(title);
                updSt.setContentMd(req.getContentMd());
                int rows = contestStatementMapper.updateById(updSt);
                if (rows <= 0) return Result.fail("更新自定义题面失败");

                ContestProblem upd = new ContestProblem();
                upd.setId(cp.getId());
                upd.setStatementMode("CUSTOM");
                contestProblemMapper.updateById(upd);
            }

            redisUtil.delete(RedisKeys.CONTEST_PROBLEMS + req.getContestId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("设置题面失败：" + e.getMessage());
        }
    }

    // ---------------- helpers ----------------

    private void insertParticipants(Long contestId, List<Long> userIds, int isOfficial) {
        if (userIds == null) return;
        for (Long uid : userIds) {
            if (uid == null) continue;
            ContestParticipant cp = new ContestParticipant();
            cp.setContestId(contestId);
            cp.setUserId(uid);
            cp.setIsOfficial(isOfficial);
            participantMapper.insert(cp);
        }
    }

    private String genNextAlias(Long contestId) {
        // 取已有 alias 集合
        List<ContestProblem> list = contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        Set<String> used = new HashSet<>();
        for (ContestProblem p : list) if (!ValidationUtil.isBlank(p.getAlias())) used.add(p.getAlias().toUpperCase(Locale.ROOT));

        for (char ch = 'A'; ch <= 'Z'; ch++) {
            String a = String.valueOf(ch);
            if (!used.contains(a)) return a;
        }
        // 超过26题：A1,A2...
        int k = 1;
        while (true) {
            String a = "A" + k;
            if (!used.contains(a)) return a;
            k++;
        }
    }

    private void fillVisibilityDefaults(Contest c, AdminContestCreateReq req) {
        c.setDuringOtherSubVisible(nvlInt(req.getDuringOtherSubVisible(), 1));
        c.setDuringStandingsVisible(nvlInt(req.getDuringStandingsVisible(), 1));
        c.setDuringPercentageVisible(nvlInt(req.getDuringPercentageVisible(), 1));
        c.setDuringTestcaseScoreVisible(nvlInt(req.getDuringTestcaseScoreVisible(), 0));

        c.setAfterOtherSubVisible(nvlInt(req.getAfterOtherSubVisible(), 1));
        c.setAfterStandingsVisible(nvlInt(req.getAfterStandingsVisible(), 1));
        c.setAfterPercentageVisible(nvlInt(req.getAfterPercentageVisible(), 1));
        c.setAfterTestcaseScoreVisible(nvlInt(req.getAfterTestcaseScoreVisible(), 1));
    }

    private void patchVisibility(Contest upd, AdminContestUpdateReq req) {
        if (req.getDuringOtherSubVisible() != null) upd.setDuringOtherSubVisible(req.getDuringOtherSubVisible());
        if (req.getDuringStandingsVisible() != null) upd.setDuringStandingsVisible(req.getDuringStandingsVisible());
        if (req.getDuringPercentageVisible() != null) upd.setDuringPercentageVisible(req.getDuringPercentageVisible());
        if (req.getDuringTestcaseScoreVisible() != null) upd.setDuringTestcaseScoreVisible(req.getDuringTestcaseScoreVisible());

        if (req.getAfterOtherSubVisible() != null) upd.setAfterOtherSubVisible(req.getAfterOtherSubVisible());
        if (req.getAfterStandingsVisible() != null) upd.setAfterStandingsVisible(req.getAfterStandingsVisible());
        if (req.getAfterPercentageVisible() != null) upd.setAfterPercentageVisible(req.getAfterPercentageVisible());
        if (req.getAfterTestcaseScoreVisible() != null) upd.setAfterTestcaseScoreVisible(req.getAfterTestcaseScoreVisible());
    }

    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
    private static Integer nvlInt(Integer v, int d) { return v == null ? d : v; }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}
