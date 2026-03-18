package com.function.neepuacmv1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminContestQueryMapper {

    @Select("""
    <script>
      SELECT id, title, mode, visibility, show_in_list, start_at, end_at, freeze_minutes, updated_at
      FROM acm_contest
      WHERE deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (title LIKE CONCAT('%',#{keyword},'%') OR CAST(id AS CHAR) LIKE CONCAT('%',#{keyword},'%'))
      </if>
      ORDER BY start_at DESC, id DESC
      LIMIT #{offset}, #{size}
    </script>
    """)
    List<Map<String,Object>> pageContests(String keyword, int offset, int size);

    @Select("""
    <script>
      SELECT COUNT(1)
      FROM acm_contest
      WHERE deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (title LIKE CONCAT('%',#{keyword},'%') OR CAST(id AS CHAR) LIKE CONCAT('%',#{keyword},'%'))
      </if>
    </script>
    """)
    long countContests(String keyword);

    @Select("""
      SELECT user_id AS userId
      FROM acm_contest_participant
      WHERE contest_id = #{contestId} AND is_official = 1
      ORDER BY user_id ASC
    """)
    List<Long> listParticipants(Long contestId);

    @Select("""
      SELECT user_id AS userId
      FROM acm_contest_participant
      WHERE contest_id = #{contestId} AND is_official = 0
      ORDER BY user_id ASC
    """)
    List<Long> listUnofficialParticipants(Long contestId);

    @Select("""
      SELECT
        id, contest_id, problem_id, problem_code, alias, weight, balloon_color, order_index,
        statement_mode, base_statement_id, custom_statement_id
      FROM acm_contest_problem
      WHERE contest_id = #{contestId}
      ORDER BY order_index ASC, id ASC
    """)
    List<Map<String,Object>> listContestProblems(Long contestId);
}
