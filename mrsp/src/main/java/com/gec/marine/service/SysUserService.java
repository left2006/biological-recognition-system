package com.gec.marine.service;

import com.gec.marine.dto.UserQueryDTO;
import com.gec.marine.entity.PageResult;
import com.gec.marine.model.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysUserService extends IService<SysUser> {

    public PageResult<SysUser> userListByPage(UserQueryDTO queryDTO);

    /**
     * 用户登录校验，支持用户名或邮箱+密码
     */
    SysUser login(String usernameOrEmail, String password);

}