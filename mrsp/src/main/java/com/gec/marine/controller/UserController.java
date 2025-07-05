package com.gec.marine.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gec.marine.dto.UserQueryDTO;
import com.gec.marine.entity.PageResult;
import com.gec.marine.entity.Result;
import com.gec.marine.model.SysUser;
import com.gec.marine.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private SysUserService sysUserService;
    @GetMapping("/doUserList")
    public Result<List<SysUser>> getAllUsers(){
        List<SysUser> sysUserList = sysUserService.list();
        return Result.ok(sysUserList);
    }


    // http:/xxx/1
    // http:/xxx/2
    // http:/xxx/3
    // 根据 ID 查询用户
    @GetMapping("/doFindUserById/{id}")
    public Result<SysUser> getUserById(@PathVariable("id") Long id)
    {
        SysUser sysUser = sysUserService.getById(id);
        return Result.ok(sysUser);
    }

    // 添加用户
    @PostMapping("/doUserAddSave")
    public Result addUser(@RequestBody SysUser sysUser){
        try {
            // 注册前查重
            QueryWrapper<SysUser> usernameQuery = new QueryWrapper<>();
            usernameQuery.eq("username", sysUser.getUsername());
            if (sysUserService.count(usernameQuery) > 0) {
                return Result.failed("用户名已存在，请更换用户名");
            }
            QueryWrapper<SysUser> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", sysUser.getEmail());
            if (sysUserService.count(emailQuery) > 0) {
                return Result.failed("邮箱已存在，请更换邮箱");
            }
            if (sysUser.getStatus() == null) {
                sysUser.setStatus(1); // 默认正常
            }
            boolean flag = sysUserService.save(sysUser);
            if(flag){
                return Result.ok();
            }else {
                return Result.failed("添加用户数据失败");
            }
        } catch (Exception e) {
            return Result.failed("注册失败: " + e.getMessage());
        }
    }

    //删除用户
    @DeleteMapping("/doUserRemove/{id}")
    public Result deleteUser(@PathVariable("id") Long id)
    {
        boolean flag = sysUserService.removeById(id);
        if(flag){
            return Result.ok();
        }else
            return Result.failed("删除用户数据失败");
    }

    @PutMapping("/doUserEditSave")
    //更新操作
    public Result updateUser(@RequestBody SysUser sysUser){
        boolean flag = sysUserService.updateById(sysUser);
        if(flag){
            return Result.ok();
        }else
            return Result.failed("更新用户失败");
    }

    /**
     * 分页查询用户列表（支持用户名、角色、状态条件查询）
     */
    @GetMapping("/doUserListByPage")
    public Result<PageResult<SysUser>> doUserListByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        try {
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);
            queryDTO.setUsername(username);
            queryDTO.setRole(role);
            queryDTO.setStatus(status);

            PageResult<SysUser> pageResult = sysUserService.userListByPage(queryDTO);
            return Result.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("查询失败：" + e.getMessage());
        }
    }

    /**
     * 用户头像上传接口（支持多用户）
     * 前端需传userId参数
     */
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       @RequestParam("userId") Long userId) {
        try {
            if (file.isEmpty()) {
                return Result.failed("请选择要上传的图片");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.failed("请上传图片文件");
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.failed("头像不能超过2MB");
            }
            String uploadDir = "uploads/images/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            String filePath = uploadDir + fileName;
            Path path = Paths.get(filePath);
            Files.write(path, file.getBytes());
            String imageUrl = "/mrsp_server/uploads/images/" + fileName;
            SysUser user = sysUserService.getById(userId);
            if (user != null) {
                user.setAvatarUrl(imageUrl);
                sysUserService.updateById(user);
            }
            return Result.ok(imageUrl);
        } catch (Exception e) {
            return Result.failed("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody SysUser loginUser) {
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getPassword() == null) {
            return Result.failed("用户名/邮箱和密码不能为空");
        }
        SysUser user = sysUserService.login(loginUser.getUsername(), loginUser.getPassword());
        if (user != null) {
            if (user.getStatus() != null) {
                if (user.getStatus() == 0) {
                    return Result.failed("账号已被禁用，请联系管理员");
                } else if (user.getStatus() == 2) {
                    return Result.failed("账号待审核，请等待管理员审核");
                } else if (user.getStatus() == 1) {
                    return Result.ok(user);
                } else {
                    return Result.failed("账号状态异常，请联系管理员");
                }
            } else {
                return Result.failed("账号状态未知，请联系管理员");
            }
        } else {
            return Result.failed("用户名/邮箱或密码错误");
        }
    }
}
