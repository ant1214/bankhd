package com.zychen.bank.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class BindCardDTO {

    @NotBlank(message = "银行卡号不能为空")
    @Pattern(regexp = "^\\d{12}$", message = "银行卡号必须是12位数字")
    private String cardId;  // 12位卡号

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 6, message = "交易密码必须是6位数字")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须是6位数字")
    private String cardPassword;  // 6位交易密码

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度2-20位")
    private String name;  // 用于验证身份

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = "身份证号格式不正确")
    private String idNumber;  // 用于验证身份
}