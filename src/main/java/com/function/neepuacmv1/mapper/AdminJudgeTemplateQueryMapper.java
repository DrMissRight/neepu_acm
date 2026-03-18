package com.function.neepuacmv1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminJudgeTemplateQueryMapper {

    @Select("""
    <script>
      SELECT id, name, type, lang, is_enabled, is_public, fork_from_id, updated_at
      FROM acm_judge_template
      WHERE deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (name LIKE CONCAT('%',#{keyword},'%') OR lang LIKE CONCAT('%',#{keyword},'%'))
      </if>
      <if test="type != null and type != ''">
        AND type = #{type}
      </if>
      <if test="lang != null and lang != ''">
        AND lang = #{lang}
      </if>
      ORDER BY updated_at DESC, id DESC
      LIMIT #{offset}, #{size}
    </script>
    """)
    List<Map<String,Object>> page(String keyword, String type, String lang, int offset, int size);

    @Select("""
    <script>
      SELECT COUNT(1)
      FROM acm_judge_template
      WHERE deleted = 0
      <if test="keyword != null and keyword != ''">
        AND (name LIKE CONCAT('%',#{keyword},'%') OR lang LIKE CONCAT('%',#{keyword},'%'))
      </if>
      <if test="type != null and type != ''">
        AND type = #{type}
      </if>
      <if test="lang != null and lang != ''">
        AND lang = #{lang}
      </if>
    </script>
    """)
    long count(String keyword, String type, String lang);

    @Select("""
      SELECT id, name, type, lang
      FROM acm_judge_template
      WHERE deleted=0 AND is_enabled=1 AND is_public=1
      ORDER BY type ASC, lang ASC, name ASC
    """)
    List<Map<String,Object>> options();
}
