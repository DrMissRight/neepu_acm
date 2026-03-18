package com.function.neepuacmv1.controller;

import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 标签分类控制器 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping("/tree")
    public Result tree() {
        return tagService.getTagTree();
    }
}
