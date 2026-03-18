package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 测试点/样例点：acm_problem_testcase */
@Data
@TableName("acm_problem_testcase")
public class ProblemTestcase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("input_path")
    private String inputPath;

    @TableField("output_path")
    private String outputPath;

    private Integer score;

    @TableField("is_sample")
    private Integer isSample;

    @TableField("is_public")
    private Integer isPublic;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("md5_in")
    private String md5In;

    @TableField("md5_out")
    private String md5Out;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
