package com.function.neepuacmv1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionQueryMapper {

    /** 当前用户在某题是否存在 AC 记录（用于题库打勾） */
    @Select("""
        SELECT COUNT(1)
        FROM acm_submission
        WHERE user_id = #{userId}
          AND problem_id = #{problemId}
          AND final_status = 'AC'
    """)
    long countAccepted(Long userId, Long problemId);

    /** 最近提交（用于题目页 Recent Submissions） */
    @Select("""
        SELECT
          id AS submissionId,
          final_status AS status,
          language AS language,
          total_time_ms AS timeMs,
          total_memory_kb AS memoryKb,
          code_length AS codeLength,
          created_at AS createdAt
        FROM acm_submission
        WHERE user_id = #{userId}
          AND problem_id = #{problemId}
        ORDER BY created_at DESC
        LIMIT #{limit}
    """)
    List<Map<String, Object>> listRecentByUserAndProblem(Long userId, Long problemId, int limit);

    @Select("""
    <script>
      SELECT
        s.id,
        s.user_id,
        u.username,
        s.problem_id,
        p.problem_code,
        p.title,
        s.status,
        s.language,
        s.time_ms,
        s.memory_kb,
        s.code_length,
        s.created_at
      FROM acm_submission s
      LEFT JOIN acm_user u ON u.id = s.user_id
      LEFT JOIN acm_problem p ON p.id = s.problem_id
      WHERE s.deleted = 0
        AND (u.deleted = 0 OR u.deleted IS NULL)
        AND (p.deleted = 0 OR p.deleted IS NULL)

      <if test="user != null and user != ''">
        AND (u.username LIKE CONCAT('%',#{user},'%')
          OR u.account LIKE CONCAT('%',#{user},'%'))
      </if>

      <if test="problem != null and problem != ''">
        AND (p.problem_code LIKE CONCAT('%',#{problem},'%')
          OR p.title LIKE CONCAT('%',#{problem},'%'))
      </if>

      <if test="status != null and status != ''">
        AND s.status = #{status}
      </if>

      <if test="language != null and language != ''">
        AND s.language = #{language}
      </if>

      ORDER BY
        <choose>
          <when test="sortBy == 'time'"> s.time_ms </when>
          <when test="sortBy == 'memory'"> s.memory_kb </when>
          <otherwise> s.created_at </otherwise>
        </choose>
        <choose>
          <when test="order == 'asc'"> ASC </when>
          <otherwise> DESC </otherwise>
        </choose>
      LIMIT #{offset}, #{size}
    </script>
    """)
    List<Map<String,Object>> page(String user, String problem, String status, String language,
                                  String sortBy, String order, int offset, int size);

    @Select("""
    <script>
      SELECT COUNT(1)
      FROM acm_submission s
      LEFT JOIN acm_user u ON u.id = s.user_id
      LEFT JOIN acm_problem p ON p.id = s.problem_id
      WHERE s.deleted = 0
      <if test="user != null and user != ''">
        AND (u.username LIKE CONCAT('%',#{user},'%')
          OR u.account LIKE CONCAT('%',#{user},'%'))
      </if>
      <if test="problem != null and problem != ''">
        AND (p.problem_code LIKE CONCAT('%',#{problem},'%')
          OR p.title LIKE CONCAT('%',#{problem},'%'))
      </if>
      <if test="status != null and status != ''">
        AND s.status = #{status}
      </if>
      <if test="language != null and language != ''">
        AND s.language = #{language}
      </if>
    </script>
    """)
    long count(String user, String problem, String status, String language);

    @Select("""
      SELECT
        s.id,
        s.user_id,
        u.username,
        s.problem_id,
        p.problem_code,
        p.title,
        s.status,
        s.language,
        s.time_ms,
        s.memory_kb,
        s.score,
        s.code_length,
        s.error_msg,
        s.code,
        s.created_at
      FROM acm_submission s
      LEFT JOIN acm_user u ON u.id = s.user_id
      LEFT JOIN acm_problem p ON p.id = s.problem_id
      WHERE s.id = #{id} AND s.deleted = 0
      LIMIT 1
    """)
    Map<String,Object> detail(Long id);

    @Select("""
      SELECT
        id, user_id, problem_id, status, language, time_ms, memory_kb, code_length, created_at
      FROM acm_submission
      WHERE deleted=0 AND problem_id=#{problemId}
      ORDER BY created_at DESC
      LIMIT #{limit}
    """)
    List<Map<String,Object>> recentByProblem(Long problemId, int limit);
}
