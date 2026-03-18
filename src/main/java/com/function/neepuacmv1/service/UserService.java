package com.function.neepuacmv1.service;

import com.function.neepuacmv1.dto.req.ChangePasswordReq;
import com.function.neepuacmv1.dto.req.UpdateProfileReq;
import com.function.neepuacmv1.entity.Result;

/** 普通用户服务 */
public interface UserService {

    Result currentUser();

    Result updateProfile(UpdateProfileReq req);

    Result changePassword(ChangePasswordReq req);
}
