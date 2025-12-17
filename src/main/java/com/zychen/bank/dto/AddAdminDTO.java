package com.zychen.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddAdminDTO {
    @NotNull(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "昵称不能为空")
    private String username;

    @NotNull(message = "密码不能为空")
    private String password;

    @NotNull(message = "姓名不能为空")
    private String name;

    @NotNull(message = "身份证号不能为空")
    private String idNumber;
}