package com.community.platform.service;

import com.community.platform.dto.*;

public interface AdminUserService {

    PageResult<AdminUserVO> list(AdminUserListQuery query);

    AdminUserVO create(AdminUserCreateRequest request);

    AdminUserVO update(AdminUserUpdateRequest request);

    void delete(Long id);

    AdminUserVO setStatus(Long id, Byte status);
}

