package com.function.neepuacmv1.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.constant.SecurityConstants;
import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;
import com.function.neepuacmv1.entity.Role;
import com.function.neepuacmv1.entity.User;
import com.function.neepuacmv1.entity.UserRole;
import com.function.neepuacmv1.mapper.AdminUserQueryMapper;
import com.function.neepuacmv1.mapper.RoleMapper;
import com.function.neepuacmv1.mapper.UserMapper;
import com.function.neepuacmv1.mapper.UserRoleMapper;
import com.function.neepuacmv1.service.AdminUserService;
import com.function.neepuacmv1.utils.PasswordUtil;
import com.function.neepuacmv1.utils.RedisUtil;
import com.function.neepuacmv1.utils.TokenUtil;
import com.function.neepuacmv1.utils.ValidationUtil;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 管理端用户服务实现 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private AdminUserQueryMapper adminUserQueryMapper;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordUtil passwordUtil;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminUserServiceImpl(UserMapper userMapper,
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
    public Result pageUsers(int page, int size, String keyword, String roleCode, Integer status) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");

            int offset = (page - 1) * size;
            long total = adminUserQueryMapper.countUsers(keyword, roleCode, status);
            List<Map<String, Object>> rows = adminUserQueryMapper.pageUsers(keyword, roleCode, status, offset, size);

            return Result.ok(rows, total);
        } catch (Exception e) {
            return Result.fail("查询用户列表失败：" + e.getMessage());
        }
    }

    /*@Override
    public Result pageUsers(int page, int size, String keyword, String roleCode, Integer status) {
        try {
            if (page <= 0 || size <= 0 || size > 200) return Result.fail("分页参数不合法");

            LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
            qw.eq(User::getDeleted, 0);

            if (!ValidationUtil.isBlank(keyword)) {
                qw.and(w -> w.like(User::getUsername, keyword)
                        .or().like(User::getEmail, keyword)
                        .or().like(User::getPhone, keyword)
                        .or().like(User::getNickname, keyword));
            }
            if (status != null) {
                qw.eq(User::getStatus, status);
            }
            qw.orderByDesc(User::getCreatedAt);

            Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
            List<User> records = p.getRecords();

            // roleCode 过滤：简单做法是再筛一遍（数据量大可写 JOIN SQL）
            if (!ValidationUtil.isBlank(roleCode)) {
                List<User> filtered = new ArrayList<>();
                for (User u : records) {
                    List<String> roles = roleMapper.listRoleCodesByUserId(u.getId());
                    if (roles != null && roles.contains(roleCode)) {
                        filtered.add(u);
                    }
                }
                return Result.ok(filtered, (long) filtered.size());
            }

            return Result.ok(records, p.getTotal());
        } catch (Exception e) {
            return Result.fail("查询用户列表失败：" + e.getMessage());
        }
    }*/

    @Override
    @Transactional
    public Result createUser(AdminCreateUserReq req) {
        try {
            if (req == null) return Result.fail("请求参数不能为空");
            if (!ValidationUtil.validUsername(req.getUsername())) return Result.fail("用户名格式不合法");
            if (!ValidationUtil.validPassword(req.getPassword())) return Result.fail("密码强度不足");
            if (!ValidationUtil.isBlank(req.getEmail()) && !ValidationUtil.validEmail(req.getEmail())) return Result.fail("邮箱格式不正确");
            if (!ValidationUtil.isBlank(req.getPhone()) && !ValidationUtil.validPhone(req.getPhone())) return Result.fail("手机号格式不正确");

            // 唯一性
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()).eq(User::getDeleted, 0)) > 0) {
                return Result.fail("用户名已存在");
            }
            if (!ValidationUtil.isBlank(req.getEmail())) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()).eq(User::getDeleted, 0)) > 0) {
                    return Result.fail("邮箱已被注册");
                }
            }
            if (!ValidationUtil.isBlank(req.getPhone())) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()).eq(User::getDeleted, 0)) > 0) {
                    return Result.fail("手机号已被注册");
                }
            }

            User u = new User();
            u.setUsername(req.getUsername());
            u.setPasswordHash(passwordUtil.hash(req.getPassword()));
            u.setEmail(req.getEmail());
            u.setPhone(req.getPhone());
            u.setNickname(ValidationUtil.isBlank(req.getNickname()) ? req.getUsername() : req.getNickname());
            u.setStatus(req.getStatus() == null ? 1 : req.getStatus());
            u.setDeleted(0);
            userMapper.insert(u);

            // 分配角色：为空则默认 TRAINEE
            List<String> roleCodes = req.getRoleCodes();
            if (roleCodes == null || roleCodes.isEmpty()) {
                roleCodes = List.of(TokenUtil.defaultRegisterRole());
            }

            for (String rc : roleCodes) {
                Role role = roleMapper.findByCode(rc);
                if (role == null) return Result.fail("角色不存在：" + rc);
                UserRole ur = new UserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }

            invalidateUserCache(u.getId());
            return Result.ok(Map.of("userId", u.getId()));
        } catch (Exception e) {
            return Result.fail("新增用户失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateUser(AdminUpdateUserReq req) {
        try {
            if (req == null || req.getUserId() == null) return Result.fail("userId不能为空");
            if (!ValidationUtil.isBlank(req.getEmail()) && !ValidationUtil.validEmail(req.getEmail())) return Result.fail("邮箱格式不正确");
            if (!ValidationUtil.isBlank(req.getPhone()) && !ValidationUtil.validPhone(req.getPhone())) return Result.fail("手机号格式不正确");

            User exist = userMapper.selectById(req.getUserId());
            if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.fail("用户不存在");

            // 若邮箱/手机有改动，做唯一性
            if (!ValidationUtil.isBlank(req.getEmail()) && !req.getEmail().equals(exist.getEmail())) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()).eq(User::getDeleted, 0)) > 0) {
                    return Result.fail("邮箱已被占用");
                }
            }
            if (!ValidationUtil.isBlank(req.getPhone()) && !req.getPhone().equals(exist.getPhone())) {
                if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()).eq(User::getDeleted, 0)) > 0) {
                    return Result.fail("手机号已被占用");
                }
            }

            User upd = new User();
            upd.setId(req.getUserId());
            upd.setEmail(req.getEmail());
            upd.setPhone(req.getPhone());
            upd.setNickname(req.getNickname());
            upd.setAvatarUrl(req.getAvatarUrl());
            upd.setRealName(req.getRealName());
            upd.setSchool(req.getSchool());
            upd.setCollege(req.getCollege());
            upd.setSignature(req.getSignature());
            upd.setStatus(req.getStatus());

            userMapper.updateById(upd);

            invalidateUserCache(req.getUserId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新用户失败：" + e.getMessage());
        }
    }

    @Override
    public Result resetPassword(AdminResetPasswordReq req) {
        try {
            if (req == null || req.getUserId() == null) return Result.fail("userId不能为空");
            if (!ValidationUtil.validPassword(req.getNewPassword())) return Result.fail("新密码强度不足");

            User exist = userMapper.selectById(req.getUserId());
            if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.fail("用户不存在");

            User upd = new User();
            upd.setId(req.getUserId());
            upd.setPasswordHash(passwordUtil.hash(req.getNewPassword()));
            userMapper.updateById(upd);

            invalidateUserCache(req.getUserId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("重置密码失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result assignRoles(AdminAssignRolesReq req) {
        try {
            if (req == null || req.getUserId() == null) return Result.fail("userId不能为空");
            if (req.getRoleCodes() == null || req.getRoleCodes().isEmpty()) return Result.fail("角色列表不能为空");

            User exist = userMapper.selectById(req.getUserId());
            if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.fail("用户不存在");

            // 删除旧角色
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, req.getUserId()));

            // 新增角色
            for (String rc : req.getRoleCodes()) {
                Role role = roleMapper.findByCode(rc);
                if (role == null) return Result.fail("角色不存在：" + rc);
                UserRole ur = new UserRole();
                ur.setUserId(req.getUserId());
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }

            // 刷新 roles 缓存
            List<String> roles = roleMapper.listRoleCodesByUserId(req.getUserId());
            redisUtil.set(RedisKeys.USER_ROLES + req.getUserId(), objectMapper.writeValueAsString(roles), SecurityConstants.ROLE_CACHE_TTL_SECONDS);

            invalidateUserCache(req.getUserId());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("分配角色失败：" + e.getMessage());
        }
    }

    @Override
    public Result deleteUser(Long userId) {
        try {
            if (userId == null) return Result.fail("userId不能为空");

            int rows = userMapper.update(
                    null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .eq(User::getDeleted, 0)
                            .set(User::getDeleted, 1)
                            .set(User::getStatus, 0)
            );
            if (rows <= 0) return Result.fail("删除失败：用户不存在或已删除");

            kickUserTokens(userId);
            invalidateUserCache(userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除用户失败：" + e.getMessage());
        }
    }

    /*@Override
    public Result deleteUser(Long userId) {
        try {
            if (userId == null) return Result.fail("userId不能为空");

            int rows = userMapper.deleteById(userId); // 逻辑删除：UPDATE ... SET deleted=1
            if (rows <= 0) return Result.fail("删除失败：用户不存在或已删除");

            // 可选：同时禁用（逻辑删除后通常无需再禁用，但你想保留也行）
            userMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .set(User::getStatus, 0)
            );

            invalidateUserCache(userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除用户失败：" + e.getMessage());
        }
    }*/


    @Override
    public Result enableUser(Long userId, Integer status) {
        try {
            if (userId == null) return Result.fail("userId不能为空");
            if (status == null || (status != 0 && status != 1)) return Result.fail("status只能为0或1");

            int rows = userMapper.update(
                    null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .eq(User::getDeleted, 0)
                            .set(User::getStatus, status)
            );
            if (rows <= 0) return Result.fail("更新失败：用户不存在或已删除");

            if (status == 0) {
                kickUserTokens(userId);
            }
            invalidateUserCache(userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新用户状态失败：" + e.getMessage());
        }
    }

    /*@Override
    public Result enableUser(Long userId, Integer status) {
        try {
            if (userId == null) return Result.fail("userId不能为空");
            if (status == null || (status != 0 && status != 1)) return Result.fail("status只能为0或1");

            User exist = userMapper.selectById(userId);
            if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.fail("用户不存在");

            User upd = new User();
            upd.setId(userId);
            upd.setStatus(status);
            userMapper.updateById(upd);

            invalidateUserCache(userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("更新用户状态失败：" + e.getMessage());
        }
    }*/

    private void invalidateUserCache(Long userId) {
        redisUtil.delete(RedisKeys.USER_INFO + userId);
        redisUtil.delete(RedisKeys.USER_ROLES + userId);
        // Token 不在此处强制失效；若禁用用户可在 enableUser(status=0) 时扩展：扫描并删除其所有 token（需额外 token->user 的反向索引）
    }

    @Override
    @Transactional
    public Result batchDelete(AdminBatchDeleteReq req) {
        try {
            if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
                return Result.fail("userIds不能为空");
            }

            for (Long uid : req.getUserIds()) {
                if (uid == null) continue;
                int rows = userMapper.update(
                        null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                                .eq(User::getId, uid)
                                .eq(User::getDeleted, 0)
                                .set(User::getDeleted, 1)
                                .set(User::getStatus, 0)
                );
                if (rows > 0) {
                    kickUserTokens(uid);
                    invalidateUserCache(uid);
                }
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("批量删除失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result importUsers(org.springframework.web.multipart.MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) return Result.fail("文件不能为空");

            // 解析 Excel：第一行为表头
            List<String> failReasons = new ArrayList<>();
            int totalRows = 0, okRows = 0;

            try (org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
                org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
                if (sheet == null) return Result.fail("Excel内容为空");

                int lastRow = sheet.getLastRowNum();
                for (int i = 1; i <= lastRow; i++) { // 从第2行开始
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    totalRows++;

                    String username = cell(row, 0);
                    String password = cell(row, 1);
                    String email = cell(row, 2);
                    String phone = cell(row, 3);
                    String nickname = cell(row, 4);
                    String roleCode = cell(row, 5); // 可选：单角色（如 MEMBER）；多角色可用逗号分隔自行扩展

                    // 基础校验（沿用你已有规则）
                    if (!com.function.neepuacmv1.utils.ValidationUtil.validUsername(username)) {
                        failReasons.add("第" + (i+1) + "行：用户名不合法");
                        continue;
                    }
                    if (!com.function.neepuacmv1.utils.ValidationUtil.validPassword(password)) {
                        failReasons.add("第" + (i+1) + "行：密码强度不足");
                        continue;
                    }
                    if (email != null && !email.isBlank() && !com.function.neepuacmv1.utils.ValidationUtil.validEmail(email)) {
                        failReasons.add("第" + (i+1) + "行：邮箱格式不正确");
                        continue;
                    }
                    if (phone != null && !phone.isBlank() && !com.function.neepuacmv1.utils.ValidationUtil.validPhone(phone)) {
                        failReasons.add("第" + (i+1) + "行：手机号格式不正确");
                        continue;
                    }

                    // 唯一性
                    if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username).eq(User::getDeleted, 0)) > 0) {
                        failReasons.add("第" + (i+1) + "行：用户名已存在");
                        continue;
                    }
                    if (email != null && !email.isBlank()) {
                        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                                .eq(User::getEmail, email).eq(User::getDeleted, 0)) > 0) {
                            failReasons.add("第" + (i+1) + "行：邮箱已存在");
                            continue;
                        }
                    }
                    if (phone != null && !phone.isBlank()) {
                        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                                .eq(User::getPhone, phone).eq(User::getDeleted, 0)) > 0) {
                            failReasons.add("第" + (i+1) + "行：手机号已存在");
                            continue;
                        }
                    }

                    // 创建用户
                    User u = new User();
                    u.setUsername(username);
                    u.setPasswordHash(passwordUtil.hash(password));
                    u.setEmail(email);
                    u.setPhone(phone);
                    u.setNickname((nickname == null || nickname.isBlank()) ? username : nickname);
                    u.setStatus(1);
                    u.setDeleted(0);
                    userMapper.insert(u);

                    // 角色：空则默认 TRAINEE
                    String rc = (roleCode == null || roleCode.isBlank()) ? com.function.neepuacmv1.utils.TokenUtil.defaultRegisterRole() : roleCode.trim();
                    Role role = roleMapper.findByCode(rc);
                    if (role == null) {
                        failReasons.add("第" + (i+1) + "行：角色不存在 " + rc + "（已创建用户，建议你决定是否回滚）");
                        // 这里选择回滚更合理：直接抛异常即可。为了给你更清晰的失败原因，这里保留记录。
                    } else {
                        UserRole ur = new UserRole();
                        ur.setUserId(u.getId());
                        ur.setRoleId(role.getId());
                        userRoleMapper.insert(ur);
                    }

                    invalidateUserCache(u.getId());
                    okRows++;
                }
            }

            com.function.neepuacmv1.dto.resp.AdminImportResultResp resp =
                    new com.function.neepuacmv1.dto.resp.AdminImportResultResp(
                            totalRows, okRows, totalRows - okRows, failReasons
                    );
            return Result.ok(resp);
        } catch (Exception e) {
            return Result.fail("批量导入失败：" + e.getMessage());
        }
    }

    private static String cell(Row row, int idx) {
        org.apache.poi.ss.usermodel.Cell c = row.getCell(idx);
        if (c == null) return null;
        c.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        String v = c.getStringCellValue();
        return v == null ? null : v.trim();
    }

    private void kickUserTokens(Long userId) {
        try {
            java.util.Set<String> tokens = redisUtil.smembers(RedisKeys.USER_TOKENS + userId);
            if (tokens != null) {
                for (String t : tokens) {
                    if (t != null && !t.isBlank()) {
                        redisUtil.delete(RedisKeys.TOKEN + t);
                    }
                }
            }
            redisUtil.delete(RedisKeys.USER_TOKENS + userId);
        } catch (Exception ignored) {}
    }
}
