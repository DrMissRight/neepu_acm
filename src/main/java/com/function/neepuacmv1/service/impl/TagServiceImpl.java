package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.dto.resp.TagTreeResp;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.Tag;
import com.function.neepuacmv1.entity.TagCategory;
import com.function.neepuacmv1.mapper.TagCategoryMapper;
import com.function.neepuacmv1.mapper.TagMapper;
import com.function.neepuacmv1.utils.RedisUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements com.function.neepuacmv1.service.TagService {

    private final TagCategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper om = new ObjectMapper();

    public TagServiceImpl(TagCategoryMapper categoryMapper, TagMapper tagMapper, RedisUtil redisUtil) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result getTagTree() {
        try {
            Object cached = redisUtil.get(RedisKeys.TAG_TREE);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<TagTreeResp> list = om.readValue(String.valueOf(cached),
                        om.getTypeFactory().constructCollectionType(List.class, TagTreeResp.class));
                return Result.ok(list);
            }

            List<TagCategory> cats = categoryMapper.selectList(new LambdaQueryWrapper<TagCategory>()
                    .orderByAsc(TagCategory::getSort).orderByAsc(TagCategory::getId));

            if (cats == null) cats = Collections.emptyList();

            List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                    .orderByAsc(Tag::getSort).orderByAsc(Tag::getId));

            Map<Long, List<Tag>> group = tags.stream().collect(Collectors.groupingBy(Tag::getCategoryId));

            List<TagTreeResp> resp = new ArrayList<>();
            for (TagCategory c : cats) {
                List<Tag> children = group.getOrDefault(c.getId(), Collections.emptyList());
                List<TagTreeResp.TagItem> items = new ArrayList<>();
                for (Tag t : children) {
                    items.add(new TagTreeResp.TagItem(t.getId(), t.getName(), t.getSort()));
                }
                resp.add(new TagTreeResp(c.getId(), c.getName(), c.getSort(), items));
            }

            // 缓存 6 小时
            redisUtil.set(RedisKeys.TAG_TREE, om.writeValueAsString(resp), 6 * 3600);
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取标签分类失败：" + e.getMessage());
        }
    }
}
