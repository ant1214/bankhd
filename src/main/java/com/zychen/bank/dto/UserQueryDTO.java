package com.zychen.bank.dto;
import lombok.Data;

@Data
public class UserQueryDTO {
    private String search;        // 搜索关键字（姓名/手机号/用户名）
    private Integer role;         // 角色：0=用户，1=管理员
    private Integer accountStatus; // 账户状态：0=正常，1=冻结
    private Integer page = 1;     // 页码，默认1
    private Integer pageSize = 20; // 每页大小，默认20
}
