package com.zychen.bank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FixedDepositDTO {
    @NotNull(message = "银行卡号不能为空")
    private String cardId;

    @NotNull(message = "本金不能为空")
    @Min(value = 100, message = "定期存款最低100元")
    private BigDecimal principal;

    @NotNull(message = "存款期限不能为空")
    private Integer term; // 3,6,12,24,36

    @NotNull(message = "交易密码不能为空")
    private String cardPassword;

    private Boolean autoRenew = false; // 默认不自动续存
}