package com.function.neepuacmv1.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.function.neepuacmv1.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 账号登录：username/email/phone 任一匹配 */
    @Select("""
        SELECT * FROM acm_user
        WHERE deleted = 0
          AND (username = #{account} OR email = #{account} OR phone = #{account})
        LIMIT 1
    """)
    User findByAccount(String account);
}
