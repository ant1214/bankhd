package com.zychen.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnbindCardDTO {
    @NotNull(message = "银行卡号不能为空")
    private String cardId;

    @NotNull(message = "交易密码不能为空")
    private String cardPassword;
}
