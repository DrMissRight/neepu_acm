package com.function.neepuacmv1.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class JudgeTemplateScriptValidator {
    private JudgeTemplateScriptValidator() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<String> validate(String type, String scriptJson) {
        List<String> errs = new ArrayList<>();
        if (ValidationUtil.isBlank(scriptJson)) {
            errs.add("Script不能为空");
            return errs;
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(scriptJson);
        } catch (Exception e) {
            errs.add("Script不是合法JSON：" + e.getMessage());
            return errs;
        }

        String t = type == null ? "" : type.trim().toUpperCase();
        if (!("IO".equals(t) || "SPJ".equals(t) || "ADVANCED".equals(t))) {
            errs.add("type必须为 IO/SPJ/Advanced");
            return errs;
        }

        // 必须有 user
        JsonNode user = root.get("user");
        if (user == null || user.isNull()) {
            errs.add("Script缺少 user 节点");
            return errs;
        }
        validateCompileRun(errs, user, "user");

        if ("SPJ".equals(t)) {
            JsonNode spj = root.get("spj");
            if (spj == null || spj.isNull()) {
                errs.add("SPJ类型 Script 缺少 spj 节点");
            } else {
                validateCompileRun(errs, spj, "spj");
            }
        }

        // Advanced：这里只做 JSON 合法性，不强制结构（文档“待写”，你可后续增强）
        return errs;
    }

    private static void validateCompileRun(List<String> errs, JsonNode node, String name) {
        JsonNode compile = node.get("compile");
        if (compile == null || compile.isNull()) {
            errs.add(name + ".compile 缺失");
        } else {
            if (isBlank(compile.get("srcName"))) errs.add(name + ".compile.srcName 缺失");
            if (compile.get("maxCpuTime") == null) errs.add(name + ".compile.maxCpuTime 缺失");
            JsonNode commands = compile.get("commands");
            if (commands == null || !commands.isArray() || commands.size() == 0) {
                errs.add(name + ".compile.commands 需为非空数组");
            }
        }

        JsonNode run = node.get("run");
        if (run == null || run.isNull()) {
            errs.add(name + ".run 缺失");
        } else {
            if (isBlank(run.get("command"))) errs.add(name + ".run.command 缺失");
        }
    }

    private static boolean isBlank(JsonNode n) {
        return n == null || n.isNull() || n.asText().trim().isEmpty();
    }
}
