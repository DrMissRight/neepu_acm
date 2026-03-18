package com.function.neepuacmv1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserQueryMapper {

    @Select("""
    <script>
      SELECT
        u.id, u.username, u.email, u.phone, u.nickname, u.status, u.deleted, u.created_at, u.updated_at
      FROM acm_user u
      <if test="roleCode != null and roleCode != ''">
        JOIN acm_user_role ur ON ur.user_id = u.id
        JOIN acm_role r ON r.id = ur.role_id AND r.role_code = #{roleCode}
      </if>
      WHERE u.deleted = 0
      <if test="status != null">
        AND u.status = #{status}
      </if>
      <if test="keyword != null and keyword != ''">
        AND (u.username LIKE CONCAT('%',#{keyword},'%')
          OR u.email LIKE CONCAT('%',#{keyword},'%')
          OR u.phone LIKE CONCAT('%',#{keyword},'%')
          OR u.nickname LIKE CONCAT('%',#{keyword},'%'))
      </if>
      ORDER BY u.created_at DESC
      LIMIT #{offset}, #{size}
    </script>
    """)
    List<Map<String, Object>> pageUsers(String keyword, String roleCode, Integer status, int offset, int size);

    @Select("""
    <script>
      SELECT COUNT(DISTINCT u.id)
      FROM acm_user u
      <if test="roleCode != null and roleCode != ''">
        JOIN acm_user_role ur ON ur.user_id = u.id
        JOIN acm_role r ON r.id = ur.role_id AND r.role_code = #{roleCode}
      </if>
      WHERE u.deleted = 0
      <if test="status != null">
        AND u.status = #{status}
      </if>
      <if test="keyword != null and keyword != ''">
        AND (u.username LIKE CONCAT('%',#{keyword},'%')
          OR u.email LIKE CONCAT('%',#{keyword},'%')
          OR u.phone LIKE CONCAT('%',#{keyword},'%')
          OR u.nickname LIKE CONCAT('%',#{keyword},'%'))
      </if>
    </script>
    """)
    long countUsers(String keyword, String roleCode, Integer status);
}
