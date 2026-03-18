package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.constant.SecurityConstants;
import com.function.neepuacmv1.dto.req.ChangePasswordReq;
import com.function.neepuacmv1.dto.req.UpdateProfileReq;
import com.function.neepuacmv1.dto.resp.CurrentUserResp;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.User;
import com.function.neepuacmv1.mapper.RoleMapper;
import com.function.neepuacmv1.mapper.UserMapper;
import com.function.neepuacmv1.security.UserContext;
import com.function.neepuacmv1.service.UserService;
import com.function.neepuacmv1.utils.PasswordUtil;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 普通用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordUtil passwordUtil;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PasswordUtil passwordUtil,
                           RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordUtil = passwordUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public Result currentUser() {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) return Result.fail("未登录");

            // 先读缓存
            Object cached = redisUtil.get(RedisKeys.USER_INFO + userId);
            if (cached != null) {
                return Result.ok(objectMapper.readValue(String.valueOf(cached), CurrentUserResp.class));
            }

            User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getId, userId).eq(User::getDeleted, 0));
            if (u == null) return Result.fail("用户不存在");

            List<String> roles = roleMapper.listRoleCodesByUserId(userId);
            CurrentUserResp resp = new CurrentUserResp(
                    u.getId(), u.getUsername(), u.getEmail(), u.getPhone(),
                    u.getNickname(), u.getAvatarUrl(), u.getRealName(), u.getSchool(),
                    u.getCollege(), u.getSignature(), u.getStatus(),
                    roles
            );

            redisUtil.set(RedisKeys.USER_INFO + userId, objectMapper.writeValueAsString(resp), SecurityConstants.USER_CACHE_TTL_SECONDS);
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("获取用户信息失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateProfile(UpdateProfileReq req) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) return Result.fail("未登录");
            if (req == null) return Result.fail("请求参数不能为空");

            User upd = new User();
            upd.setId(userId);
            upd.setNickname(req.getNickname());
            upd.setAvatarUrl(req.getAvatarUrl());
            upd.setRealName(req.getRealName());
            upd.setSchool(req.getSchool());
            upd.setCollege(req.getCollege());
            upd.setSignature(req.getSignature());

            userMapper.updateById(upd);

            // 清缓存
            redisUtil.delete(RedisKeys.USER_INFO + userId);

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新个人信息失败：" + e.getMessage());
        }
    }

    @Override
    public Result changePassword(ChangePasswordReq req) {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) return Result.fail("未登录");
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getOldPassword()) || ValidationUtil.isBlank(req.getNewPassword())) {
                return Result.fail("旧密码/新密码不能为空");
            }
            if (!ValidationUtil.validPassword(req.getNewPassword())) {
                return Result.fail("新密码强度不足（8-64位且包含字母与数字）");
            }

            User u = userMapper.selectById(userId);
            if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)) return Result.fail("用户不存在");

            if (!passwordUtil.matches(req.getOldPassword(), u.getPasswordHash())) {
                return Result.fail("旧密码不正确");
            }

            User upd = new User();
            upd.setId(userId);
            upd.setPasswordHash(passwordUtil.hash(req.getNewPassword()));
            userMapper.updateById(upd);

            // 清缓存
            redisUtil.delete(RedisKeys.USER_INFO + userId);

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("修改密码失败：" + e.getMessage());
        }
    }
}
