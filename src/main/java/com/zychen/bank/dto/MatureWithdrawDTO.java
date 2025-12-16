package com.zychen.bank.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatureWithdrawDTO {
    @NotNull(message = "交易密码不能为空")
    private String cardPassword;
}