package com.function.neepuacmv1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminProblemQueryMapper {

    @Select("""
    <script>
      SELECT
        p.id, p.problem_code, p.title, p.is_public, p.judge_template_id,
        p.statement_count, p.accepted_count, p.submitted_count, p.updated_at
      FROM acm_problem p
      WHERE p.deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (p.problem_code LIKE CONCAT('%',#{keyword},'%')
          OR p.title LIKE CONCAT('%',#{keyword},'%'))
      </if>
      ORDER BY p.updated_at DESC
      LIMIT #{offset}, #{size}
    </script>
    """)
    List<Map<String,Object>> pageProblems(String keyword, int offset, int size);

    @Select("""
    <script>
      SELECT COUNT(1)
      FROM acm_problem p
      WHERE p.deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (p.problem_code LIKE CONCAT('%',#{keyword},'%')
          OR p.title LIKE CONCAT('%',#{keyword},'%'))
      </if>
    </script>
    """)
    long countProblems(String keyword);

    @Select("""
      SELECT id, problem_id, lang, version_name, title, is_default, is_public, updated_at
      FROM acm_problem_statement
      WHERE problem_id = #{problemId}
      ORDER BY is_default DESC, updated_at DESC
    """)
    List<Map<String,Object>> listStatements(Long problemId);

    @Select("""
      SELECT id, problem_id, is_sample, is_public, score, order_index, input_path, output_path, updated_at
      FROM acm_problem_testcase
      WHERE problem_id = #{problemId}
      ORDER BY is_sample DESC, order_index ASC, id ASC
    """)
    List<Map<String,Object>> listTestcases(Long problemId);
}
