package com.gec.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.marine.mapper.SysUserMapper;
import com.gec.marine.model.SysUser;
import com.gec.marine.service.SysUserService;
import com.gec.marine.dto.UserQueryDTO;
import com.gec.marine.entity.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public PageResult<SysUser> userListByPage(UserQueryDTO queryDTO) {
        try {
            // 参数校验
            if (queryDTO == null) {
                throw new IllegalArgumentException("查询参数不能为空");
            }
            if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
                queryDTO.setPageNum(1);
            }
            if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
                queryDTO.setPageSize(10);
            }

            // 创建分页对象
            Page<SysUser> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

            // 构建查询条件
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

            // 用户名模糊查询
            if (StringUtils.hasText(queryDTO.getUsername())) {
                queryWrapper.like(SysUser::getUsername, queryDTO.getUsername());
            }

            // 角色精确查询
            if (StringUtils.hasText(queryDTO.getRole())) {
                queryWrapper.eq(SysUser::getRole, queryDTO.getRole());
            }

            // 状态精确查询
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(SysUser::getStatus, queryDTO.getStatus());
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc(SysUser::getCreatedTime);

            // 执行分页查询
            IPage<SysUser> pageResult = sysUserMapper.selectPage(page, queryWrapper);

            // 返回分页结果
            return new PageResult<>(
                    pageResult.getRecords(),
                    pageResult.getTotal(),
                    pageResult.getCurrent(),
                    pageResult.getSize()
            );
        } catch (Exception e) {
            throw new RuntimeException("查询用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录校验
     */
    @Override
    public SysUser login(String usernameOrEmail, String password) {
        if (!StringUtils.hasText(usernameOrEmail) || !StringUtils.hasText(password)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper ->
            wrapper.eq(SysUser::getUsername, usernameOrEmail)
                   .or()
                   .eq(SysUser::getEmail, usernameOrEmail)
        );
        SysUser user = sysUserMapper.selectOne(queryWrapper);
        if (user == null) {
            return null;
        }
        if (!password.equals(user.getPassword())) {
            return null;
        }
        // 直接返回user，controller判断status
        return user;
    }

}