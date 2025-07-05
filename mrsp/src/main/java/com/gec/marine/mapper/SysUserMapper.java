package com.gec.marine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.marine.model.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // MyBatis-Plus 会自动实现常见的 CRUD 操作（insert, delete, update, select）
}