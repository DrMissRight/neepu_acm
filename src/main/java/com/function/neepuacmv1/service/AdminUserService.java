package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.*;
import com.function.neepuacmv1.entity.Result;
import org.springframework.web.multipart.MultipartFile;

/** 管理端用户服务 */
public interface AdminUserService {

    Result pageUsers(int page, int size, String keyword, String roleCode, Integer status);

    Result createUser(AdminCreateUserReq req);

    Result updateUser(AdminUpdateUserReq req);

    Result resetPassword(AdminResetPasswordReq req);

    Result assignRoles(AdminAssignRolesReq req);

    Result deleteUser(Long userId);

    Result enableUser(Long userId, Integer status);

    Result batchDelete(AdminBatchDeleteReq req);

    Result importUsers(MultipartFile file);
}
