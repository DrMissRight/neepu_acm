package com.function.neepuacmv1.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户实体：acm_user */
@Data
@TableName("acm_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密码哈希 */
    @TableField("password_hash")
    private String passwordHash;

    private String email;
    private String phone;

    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("real_name")
    private String realName;

    private String school;
    private String college;
    private String signature;

    /** 1启用 0禁用 */
    private Integer status;

    /** 软删：1删除 0正常 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT, value = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "updated_at")
    private LocalDateTime updatedAt;
}
