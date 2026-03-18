package com.function.neepuacmv1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.function.neepuacmv1.entity.ProblemTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProblemTagMapper extends BaseMapper<ProblemTag> {

    @Select("""
                SELECT t.name
                FROM acm_problem_tag pt
                JOIN acm_tag t ON t.id = pt.tag_id
                WHERE pt.problem_id = #{problemId}
                ORDER BY t.sort ASC, t.id ASC
            """)
    List<String> listTagNamesByProblemId(Long problemId);
}
