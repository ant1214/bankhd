package com.zychen.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class LoginDTO {


    @NotBlank(message = "用户名/手机号不能为空")
    private String account;  // 可以是用户名或手机号

    @NotBlank(message = "密码不能为空")
    private String password;
}
