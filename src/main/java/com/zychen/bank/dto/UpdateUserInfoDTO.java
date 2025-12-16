package com.zychen.bank.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class UpdateUserInfoDTO {
    // 用户表可更新字段
    private String phone;  // 手机号

    // 用户信息表可更新字段
    private String name;      // 姓名
    private Integer gender;   // 性别：0=女，1=男
    private LocalDate birthDate;   // 出生日期
    private String email;     // 邮箱
    private String address;   // 地址

}
