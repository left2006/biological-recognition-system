package com.gec.marine.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private String username;    // 用户名查询条件
    private String role;        // 角色查询条件
    private Integer status;     // 状态查询条件
    private Integer pageNum = 1;    // 当前页码，默认第1页
    private Integer pageSize = 10;  // 每页显示数量，默认10条

    // Getters and Setters
    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}