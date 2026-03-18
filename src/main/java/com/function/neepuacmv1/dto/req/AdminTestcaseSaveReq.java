package com.function.neepuacmv1.dto.req;

import lombok.Data;

import java.util.List;

/** Save：批量保存测试点/样例点顺序与属性 */
@Data
public class AdminTestcaseSaveReq {
    private Long problemId;

    /** 全量提交（包含样例/普通测试点） */
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Integer isSample;   // 1/0
        private Integer isPublic;   // 1/0
        private Integer score;      // 分值
        private Integer orderIndex; // 顺序
    }
}
