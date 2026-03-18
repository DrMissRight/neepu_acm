package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.constant.SecurityConstants;
import com.function.neepuacmv1.dto.req.LoginReq;
import com.function.neepuacmv1.dto.req.RegisterReq;
import com.function.neepuacmv1.dto.resp.LoginResp;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.Role;
import com.function.neepuacmv1.entity.User;
import com.function.neepuacmv1.entity.UserRole;
import com.function.neepuacmv1.mapper.RoleMapper;
import com.function.neepuacmv1.mapper.UserMapper;
import com.function.neepuacmv1.mapper.UserRoleMapper;
import com.function.neepuacmv1.service.AuthService;
import com.function.neepuacmv1.utils.PasswordUtil;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.TokenUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 认证实现：
 * - 注册：唯一性校验 + BCrypt + 默认角色(TRAINEE) + 缓存失效
 * - 登录：失败次数限制 + 状态校验 + 写 token/roles 到 Redis
 * - 登出：删除 token
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordUtil passwordUtil;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordUtil passwordUtil,
                           RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordUtil = passwordUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    @Transactional
    public Result register(RegisterReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (!ValidationUtil.validUsername(req.getUsername())) {
                return Result.fail("用户名格式不合法（4-32位字母数字下划线）");
            }
            if (!ValidationUtil.validPassword(req.getPassword())) {
                return Result.fail("密码强度不足（8-64位且包含字母与数字）");
            }
            if (!ValidationUtil.isBlank(req.getEmail()) && !ValidationUtil.validEmail(req.getEmail())) {
                return Result.fail("邮箱格式不正确");
            }
            if (!ValidationUtil.isBlank(req.getPhone()) && !ValidationUtil.validPhone(req.getPhone())) {
                return Result.fail("手机号格式不正确");
            }

            // 可选：验证码校验（短信/邮箱/图形统一用 verifyKey+code）
            if (!ValidationUtil.isBlank(req.getVerifyKey())) {
                Object code = redisUtil.get(RedisKeys.VERIFY_CODE + req.getVerifyKey());
                if (code == null || !String.valueOf(code).equalsIgnoreCase(String.valueOf(req.getVerifyCode()))) {
                    return Result.fail("验证码错误或已过期");
                }
            }

            // 唯一性校验
            Long cntUsername = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, req.getUsername()).eq(User::getDeleted, 0));
            if (cntUsername != null && cntUsername > 0) return Result.fail("用户名已存在");

            if (!ValidationUtil.isBlank(req.getEmail())) {
                Long cntEmail = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, req.getEmail()).eq(User::getDeleted, 0));
                if (cntEmail != null && cntEmail > 0) return Result.fail("邮箱已被注册");
            }
            if (!ValidationUtil.isBlank(req.getPhone())) {
                Long cntPhone = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, req.getPhone()).eq(User::getDeleted, 0));
                if (cntPhone != null && cntPhone > 0) return Result.fail("手机号已被注册");
            }

            User u = new User();
            u.setUsername(req.getUsername());
            u.setPasswordHash(passwordUtil.hash(req.getPassword()));
            u.setEmail(req.getEmail());
            u.setPhone(req.getPhone());
            u.setNickname(ValidationUtil.isBlank(req.getNickname()) ? req.getUsername() : req.getNickname());
            u.setStatus(1);
            u.setDeleted(0);
            userMapper.insert(u);

            // 默认角色：TRAINEE（更贴合集训队“预备队员”）
            String defaultRoleCode = TokenUtil.defaultRegisterRole();
            Role role = roleMapper.findByCode(defaultRoleCode);
            if (role == null) {
                return Result.fail("系统缺少默认角色配置：" + defaultRoleCode);
            }
            UserRole ur = new UserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);

            // 清理缓存
            redisUtil.delete(RedisKeys.USER_INFO + u.getId());
            redisUtil.delete(RedisKeys.USER_ROLES + u.getId());

            return Result.ok(Map.of("userId", u.getId(), "username", u.getUsername()));
        } catch (Exception e) {
            return Result.fail("注册失败：" + e.getMessage());
        }
    }

    @Override
    public Result login(LoginReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getAccount()) || ValidationUtil.isBlank(req.getPassword())) {
                return Result.fail("账号或密码不能为空");
            }

            // 可选：图形验证码校验
            if (!ValidationUtil.isBlank(req.getCaptchaId())) {
                Object code = redisUtil.get(RedisKeys.CAPTCHA + req.getCaptchaId());
                if (code == null || !String.valueOf(code).equalsIgnoreCase(String.valueOf(req.getCaptchaCode()))) {
                    return Result.fail("图形验证码错误或已过期");
                }
            }

            // 失败次数限制
            Object failObj = redisUtil.get(RedisKeys.LOGIN_FAIL + req.getAccount());
            if (failObj != null) {
                try {
                    long fail = Long.parseLong(String.valueOf(failObj));
                    if (fail >= SecurityConstants.MAX_LOGIN_FAIL) {
                        return Result.fail("密码错误次数过多，请稍后再试");
                    }
                } catch (Exception ignored) {}
            }

            User user = userMapper.findByAccount(req.getAccount());
            if (user == null) {
                redisUtil.incr(RedisKeys.LOGIN_FAIL + req.getAccount(), SecurityConstants.LOGIN_FAIL_TTL_SECONDS);
                return Result.fail("账号或密码错误");
            }
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                return Result.fail("账号不存在或已删除");
            }
            if (user.getStatus() == null || user.getStatus() == 0) {
                return Result.fail("账号已被禁用，请联系管理员");
            }

            if (!passwordUtil.matches(req.getPassword(), user.getPasswordHash())) {
                redisUtil.incr(RedisKeys.LOGIN_FAIL + req.getAccount(), SecurityConstants.LOGIN_FAIL_TTL_SECONDS);
                return Result.fail("账号或密码错误");
            }

            // 登录成功：清除失败计数
            redisUtil.delete(RedisKeys.LOGIN_FAIL + req.getAccount());

            // 读取角色
            List<String> roles = roleMapper.listRoleCodesByUserId(user.getId());
            if (roles == null) roles = Collections.emptyList();

            // 生成 token 并写 Redis
            String token = TokenUtil.generateToken();
            redisUtil.set(RedisKeys.TOKEN + token, String.valueOf(user.getId()), SecurityConstants.TOKEN_TTL_SECONDS);

            // ★新增：维护 userId -> tokens(set)，用于禁用/删除时踢下线
            redisUtil.sadd(RedisKeys.USER_TOKENS + user.getId(), token);
            // 刷新 set 的 TTL，保证不短于 token（简单做法）
            redisUtil.expire(RedisKeys.USER_TOKENS + user.getId(), SecurityConstants.TOKEN_TTL_SECONDS);

            // 写 roles 缓存（JSON 数组）
            redisUtil.set(RedisKeys.USER_ROLES + user.getId(),
                    objectMapper.writeValueAsString(roles),
                    SecurityConstants.ROLE_CACHE_TTL_SECONDS);

            // 更新 lastLoginAt
            User upd = new User();
            upd.setId(user.getId());
            upd.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(upd);

            LoginResp resp = new LoginResp(token, user.getId(), user.getUsername(), user.getNickname(), roles);
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("登录失败：" + e.getMessage());
        }
    }

    /*@Override
    public Result login(LoginReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (ValidationUtil.isBlank(req.getAccount()) || ValidationUtil.isBlank(req.getPassword())) {
                return Result.fail("账号或密码不能为空");
            }

            // 可选：图形验证码校验
            if (!ValidationUtil.isBlank(req.getCaptchaId())) {
                Object code = redisUtil.get(RedisKeys.CAPTCHA + req.getCaptchaId());
                if (code == null || !String.valueOf(code).equalsIgnoreCase(String.valueOf(req.getCaptchaCode()))) {
                    return Result.fail("图形验证码错误或已过期");
                }
            }

            // 失败次数限制
            Object failObj = redisUtil.get(RedisKeys.LOGIN_FAIL + req.getAccount());
            if (failObj != null) {
                try {
                    long fail = Long.parseLong(String.valueOf(failObj));
                    if (fail >= SecurityConstants.MAX_LOGIN_FAIL) {
                        return Result.fail("密码错误次数过多，请稍后再试");
                    }
                } catch (Exception ignored) {}
            }

            User user = userMapper.findByAccount(req.getAccount());
            if (user == null) {
                redisUtil.incr(RedisKeys.LOGIN_FAIL + req.getAccount(), SecurityConstants.LOGIN_FAIL_TTL_SECONDS);
                return Result.fail("账号或密码错误");
            }
            if (user.getDeleted() != null && user.getDeleted() == 1) {
                return Result.fail("账号不存在或已删除");
            }
            if (user.getStatus() == null || user.getStatus() == 0) {
                return Result.fail("账号已被禁用，请联系管理员");
            }

            if (!passwordUtil.matches(req.getPassword(), user.getPasswordHash())) {
                redisUtil.incr(RedisKeys.LOGIN_FAIL + req.getAccount(), SecurityConstants.LOGIN_FAIL_TTL_SECONDS);
                return Result.fail("账号或密码错误");
            }

            // 登录成功：清除失败计数
            redisUtil.delete(RedisKeys.LOGIN_FAIL + req.getAccount());

            // 读取角色
            List<String> roles = roleMapper.listRoleCodesByUserId(user.getId());
            if (roles == null) roles = Collections.emptyList();

            // 生成 token 并写 Redis
            String token = TokenUtil.generateToken();
            redisUtil.set(RedisKeys.TOKEN + token, String.valueOf(user.getId()), SecurityConstants.TOKEN_TTL_SECONDS);

            // 写 roles 缓存（JSON 数组）
            redisUtil.set(RedisKeys.USER_ROLES + user.getId(), objectMapper.writeValueAsString(roles), SecurityConstants.ROLE_CACHE_TTL_SECONDS);

            // 更新 lastLoginAt
            User upd = new User();
            upd.setId(user.getId());
            upd.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(upd);

            LoginResp resp = new LoginResp(token, user.getId(), user.getUsername(), user.getNickname(), roles);
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("登录失败：" + e.getMessage());
        }
    }*/

    /*@Override
    public Result logout(String token) {
        try {
            if (ValidationUtil.isBlank(token)) return Result.fail("Token不能为空");
            boolean ok = redisUtil.delete(RedisKeys.TOKEN + token);
            return ok ? Result.ok() : Result.ok(); // 幂等
        } catch (Exception e) {
            return Result.fail("登出失败：" + e.getMessage());
        }
    }*/

    @Override
    public Result logout(String token) {
        try {
            if (ValidationUtil.isBlank(token)) return Result.fail("Token不能为空");

            // 先取 userId（否则删掉 token 后就取不到了）
            Object userIdObj = redisUtil.get(RedisKeys.TOKEN + token);

            // 删除 token 会话（幂等）
            redisUtil.delete(RedisKeys.TOKEN + token);

            // ★新增：从 userId->tokens(set) 移除该 token
            if (userIdObj != null) {
                Long userId = Long.valueOf(String.valueOf(userIdObj));
                redisUtil.srem(RedisKeys.USER_TOKENS + userId, token);
            }

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("登出失败：" + e.getMessage());
        }
    }


    @Override
    public Result sendVerifyCode(String verifyKey) {
        try {
            if (ValidationUtil.isBlank(verifyKey)) return Result.fail("verifyKey不能为空");
            String code = String.valueOf((int)(Math.random() * 900000 + 100000)); // 6位
            redisUtil.set(RedisKeys.VERIFY_CODE + verifyKey, code, SecurityConstants.VERIFY_CODE_TTL_SECONDS);

            // 生产环境：这里应调用短信/邮件服务发送 code；不应直接返回 code
            return Result.ok(Map.of("verifyKey", verifyKey, "code", code));
        } catch (Exception e) {
            return Result.fail("发送验证码失败：" + e.getMessage());
        }
    }

    @Override
    public Result generateCaptcha() {
        try {
            String captchaId = TokenUtil.generateToken();
            String code = String.valueOf((int)(Math.random() * 9000 + 1000)); // 4位
            redisUtil.set(RedisKeys.CAPTCHA + captchaId, code, SecurityConstants.CAPTCHA_TTL_SECONDS);

            // 生产环境：应返回图片/BASE64；这里返回 code 方便联调
            return Result.ok(Map.of("captchaId", captchaId, "code", code));
        } catch (Exception e) {
            return Result.fail("生成图形验证码失败：" + e.getMessage());
        }
    }
}
