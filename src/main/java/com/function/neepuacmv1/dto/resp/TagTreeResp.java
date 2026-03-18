package com.function.neepuacmv1.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 标签分类树（大类 -> 子标签） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagTreeResp {

    private Long categoryId;
    private String categoryName;
    private Integer sort;
    private List<TagItem> children;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagItem {
        private Long tagId;
        private String tagName;
        private Integer sort;
    }
}
