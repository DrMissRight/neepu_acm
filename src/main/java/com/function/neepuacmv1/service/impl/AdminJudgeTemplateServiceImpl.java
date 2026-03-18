package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.dto.resp.AdminJudgeTemplateDetailResp;
import com.function.neepuacmv1.dto.resp.AdminJudgeTemplateListItemResp;
import com.function.neepuacmv1.entity.JudgeTemplate;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.mapper.AdminJudgeTemplateQueryMapper;
import com.function.neepuacmv1.mapper.JudgeTemplateMapper;
import com.function.neepuacmv1.service.AdminJudgeTemplateService;
import com.function.neepuacmv1.utils.JudgeTemplateScriptValidator;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AdminJudgeTemplateServiceImpl implements AdminJudgeTemplateService {

    private final JudgeTemplateMapper mapper;
    private final AdminJudgeTemplateQueryMapper queryMapper;
    private final RedisUtil redisUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // TTL（秒）
    private static final long DETAIL_TTL = 300;
    private static final long OPTIONS_TTL = 300;

    public AdminJudgeTemplateServiceImpl(JudgeTemplateMapper mapper,
                                         AdminJudgeTemplateQueryMapper queryMapper,
                                         RedisUtil redisUtil) {
        this.mapper = mapper;
        this.queryMapper = queryMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result page(int page, int size, String keyword, String type, String lang) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");
            int offset = (page - 1) * size;

            long total = queryMapper.count(keyword, type, lang);
            List<Map<String,Object>> rows = queryMapper.page(keyword, type, lang, offset, size);

            List<AdminJudgeTemplateListItemResp> list = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                list.add(new AdminJudgeTemplateListItemResp(
                        toLong(r.get("id")),
                        str(r.get("name")),
                        str(r.get("type")),
                        str(r.get("lang")),
                        toInt(r.get("is_enabled")),
                        toInt(r.get("is_public")),
                        toLong(r.get("fork_from_id")),
                        (java.time.LocalDateTime) r.get("updated_at")
                ));
            }
            return Result.ok(list, total);
        } catch (Exception e) {
            return Result.fail("获取评测模板列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result detail(Long id) {
        try {
            if (id == null) return Result.fail("id不能为空");

            String key = RedisKeys.JUDGE_TEMPLATE_DETAIL + id;
            Object cached = redisUtil.get(key);
            if (cached != null && !String.valueOf(cached).isBlank()) {
                AdminJudgeTemplateDetailResp resp =
                        objectMapper.readValue(String.valueOf(cached), AdminJudgeTemplateDetailResp.class);
                return Result.ok(resp);
            }

            JudgeTemplate jt = mapper.selectOne(new LambdaQueryWrapper<JudgeTemplate>()
                    .eq(JudgeTemplate::getId, id)
                    .eq(JudgeTemplate::getDeleted, 0));
            if (jt == null) return Result.fail("模板不存在");

            AdminJudgeTemplateDetailResp resp = new AdminJudgeTemplateDetailResp(
                    jt.getId(), jt.getName(), jt.getType(), jt.getLang(), jt.getScriptJson(),
                    jt.getDescription(), jt.getIsEnabled(), jt.getIsPublic(), jt.getForkFromId(),
                    jt.getCreatedAt(), jt.getUpdatedAt()
            );

            redisUtil.set(key, objectMapper.writeValueAsString(resp), DETAIL_TTL);
            return Result.ok(resp);
        } catch (Exception e) {
            // 注意：这里如果缓存 JSON 解析失败，也不会影响 DB 回源（你也可以选择 delete key 再回源）
            return Result.fail("获取模板详情失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result create(AdminJudgeTemplateCreateReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getName())) return Result.fail("名称不能为空");
            if (ValidationUtil.isBlank(req.getType())) return Result.fail("类型不能为空");
            if (ValidationUtil.isBlank(req.getScriptJson())) return Result.fail("Script不能为空");

            String type = normalizeType(req.getType());
            if (type == null) return Result.fail("类型必须为 IO/SPJ/Advanced");

            // 名称唯一
            Long cnt = mapper.selectCount(new LambdaQueryWrapper<JudgeTemplate>()
                    .eq(JudgeTemplate::getName, req.getName().trim())
                    .eq(JudgeTemplate::getDeleted, 0));
            if (cnt != null && cnt > 0) return Result.fail("模板名称已存在");

            // Script JSON 校验（Advanced 只校验 JSON 合法性；IO/SPJ 校验结构）
            List<String> errs = JudgeTemplateScriptValidator.validate(type, req.getScriptJson());
            if (!errs.isEmpty()) return Result.fail("Script校验失败：" + String.join("; ", errs));

            JudgeTemplate jt = new JudgeTemplate();
            jt.setName(req.getName().trim());
            jt.setType(type);
            jt.setLang(ValidationUtil.isBlank(req.getLang()) ? null : req.getLang().trim());
            jt.setScriptJson(req.getScriptJson());
            jt.setDescription(req.getDescription());
            jt.setIsEnabled(req.getIsEnabled() == null ? 1 : req.getIsEnabled());
            jt.setIsPublic(req.getIsPublic() == null ? 1 : req.getIsPublic());
            jt.setForkFromId(null);
            jt.setCreatedBy(null);
            jt.setDeleted(0);

            int rows = mapper.insert(jt);
            if (rows <= 0) return Result.fail("新增模板失败");

            invalidateCache(jt.getId());
            return Result.ok(Collections.singletonMap("id", jt.getId()));
        } catch (Exception e) {
            return Result.fail("新增模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result update(AdminJudgeTemplateUpdateReq req) {
        try {
            if (req == null || req.getId() == null) return Result.fail("id不能为空");

            JudgeTemplate exist = mapper.selectOne(new LambdaQueryWrapper<JudgeTemplate>()
                    .eq(JudgeTemplate::getId, req.getId())
                    .eq(JudgeTemplate::getDeleted, 0));
            if (exist == null) return Result.fail("模板不存在");

            // 改名：唯一
            if (!ValidationUtil.isBlank(req.getName())) {
                String newName = req.getName().trim();
                Long cnt = mapper.selectCount(new LambdaQueryWrapper<JudgeTemplate>()
                        .eq(JudgeTemplate::getName, newName)
                        .eq(JudgeTemplate::getDeleted, 0)
                        .ne(JudgeTemplate::getId, req.getId()));
                if (cnt != null && cnt > 0) return Result.fail("模板名称已存在");
            }

            String newType = exist.getType();
            if (req.getType() != null) {
                String nt = normalizeType(req.getType());
                if (nt == null) return Result.fail("类型必须为 IO/SPJ/Advanced");
                newType = nt;
            }

            String newScript = req.getScriptJson() != null ? req.getScriptJson() : exist.getScriptJson();
            // 若改了 type 或 script，重新校验
            if (req.getType() != null || req.getScriptJson() != null) {
                List<String> errs = JudgeTemplateScriptValidator.validate(newType, newScript);
                if (!errs.isEmpty()) return Result.fail("Script校验失败：" + String.join("; ", errs));
            }

            JudgeTemplate upd = new JudgeTemplate();
            upd.setId(req.getId());
            if (!ValidationUtil.isBlank(req.getName())) upd.setName(req.getName().trim());
            if (req.getType() != null) upd.setType(newType);
            if (req.getLang() != null) upd.setLang(ValidationUtil.isBlank(req.getLang()) ? null : req.getLang().trim());
            if (req.getScriptJson() != null) upd.setScriptJson(req.getScriptJson());
            if (req.getDescription() != null) upd.setDescription(req.getDescription());
            if (req.getIsEnabled() != null) upd.setIsEnabled(req.getIsEnabled());
            if (req.getIsPublic() != null) upd.setIsPublic(req.getIsPublic());

            int rows = mapper.updateById(upd);
            if (rows <= 0) return Result.fail("更新失败");

            invalidateCache(req.getId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result fork(AdminJudgeTemplateForkReq req) {
        try {
            if (req == null || req.getSourceId() == null) return Result.fail("sourceId不能为空");
            if (ValidationUtil.isBlank(req.getNewName())) return Result.fail("newName不能为空");

            JudgeTemplate src = mapper.selectOne(new LambdaQueryWrapper<JudgeTemplate>()
                    .eq(JudgeTemplate::getId, req.getSourceId())
                    .eq(JudgeTemplate::getDeleted, 0));
            if (src == null) return Result.fail("源模板不存在");

            // 新名称唯一
            Long cnt = mapper.selectCount(new LambdaQueryWrapper<JudgeTemplate>()
                    .eq(JudgeTemplate::getName, req.getNewName().trim())
                    .eq(JudgeTemplate::getDeleted, 0));
            if (cnt != null && cnt > 0) return Result.fail("模板名称已存在");

            JudgeTemplate jt = new JudgeTemplate();
            jt.setName(req.getNewName().trim());
            jt.setType(src.getType());
            jt.setLang(ValidationUtil.isBlank(req.getNewLang()) ? src.getLang() : req.getNewLang().trim());
            jt.setScriptJson(src.getScriptJson());
            jt.setDescription(req.getNewDescription() != null ? req.getNewDescription() : src.getDescription());
            jt.setIsEnabled(req.getIsEnabled() == null ? src.getIsEnabled() : req.getIsEnabled());
            jt.setIsPublic(req.getIsPublic() == null ? src.getIsPublic() : req.getIsPublic());
            jt.setForkFromId(src.getId());
            jt.setCreatedBy(null);
            jt.setDeleted(0);

            int rows = mapper.insert(jt);
            if (rows <= 0) return Result.fail("Fork失败");

            invalidateCache(jt.getId());
            return Result.ok(Collections.singletonMap("id", jt.getId()));
        } catch (Exception e) {
            return Result.fail("Fork失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result deleteBatch(AdminBatchIdsReq req) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) return Result.fail("ids不能为空");

            for (Long id : req.getIds()) {
                if (id == null) continue;
                mapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<JudgeTemplate>()
                                .eq(JudgeTemplate::getId, id)
                                .eq(JudgeTemplate::getDeleted, 0)
                                .set(JudgeTemplate::getDeleted, 1)
                                .set(JudgeTemplate::getIsEnabled, 0)
                );
                invalidateCache(id);
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除模板失败：" + e.getMessage());
        }
    }

    @Override
    public Result options() {
        try {
            String key = RedisKeys.JUDGE_TEMPLATE_OPTIONS + "all";
            Object cached = redisUtil.get(key);
            if (cached != null && !String.valueOf(cached).isBlank()) {
                List<Map<String,Object>> rows = objectMapper.readValue(
                        String.valueOf(cached),
                        new TypeReference<List<Map<String,Object>>>() {}
                );
                return Result.ok(rows);
            }

            List<Map<String,Object>> rows = queryMapper.options();
            redisUtil.set(key, objectMapper.writeValueAsString(rows), OPTIONS_TTL);
            return Result.ok(rows);
        } catch (Exception e) {
            return Result.fail("获取模板选项失败：" + e.getMessage());
        }
    }

    private void invalidateCache(Long id) {
        redisUtil.delete(RedisKeys.JUDGE_TEMPLATE_DETAIL + id);
        redisUtil.delete(RedisKeys.JUDGE_TEMPLATE_OPTIONS + "all");
    }

    private static String normalizeType(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.equalsIgnoreCase("IO")) return "IO";
        if (t.equalsIgnoreCase("SPJ")) return "SPJ";
        if (t.equalsIgnoreCase("ADVANCED") || t.equalsIgnoreCase("Advanced")) return "Advanced";
        return null;
    }

    private static String str(Object o){ return o==null?null:String.valueOf(o); }
    private static Integer toInt(Object o){ try { return o==null?null:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; } }
    private static Long toLong(Object o){ try { return o==null?null:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return null; } }
}