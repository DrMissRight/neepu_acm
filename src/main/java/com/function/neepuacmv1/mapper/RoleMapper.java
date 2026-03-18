package com.function.neepuacmv1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.function.neepuacmv1.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM acm_role WHERE role_code = #{roleCode} LIMIT 1")
    Role findByCode(String roleCode);

    @Select("""
        SELECT r.role_code
        FROM acm_role r
        JOIN acm_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
    """)
    List<String> listRoleCodesByUserId(Long userId);
}
