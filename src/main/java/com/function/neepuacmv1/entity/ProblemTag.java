package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 题目-标签关联：acm_problem_tag */
@Data
@TableName("acm_problem_tag")
public class ProblemTag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("tag_id")
    private Long tagId;
}
