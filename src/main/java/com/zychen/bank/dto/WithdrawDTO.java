package com.zychen.bank.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class WithdrawDTO {

    @NotBlank(message = "银行卡号不能为空")
    private String cardId;  // 银行卡号

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 6, message = "交易密码必须是6位数字")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须是6位数字")
    private String cardPassword;  // 交易密码

    @NotNull(message = "取款金额不能为空")
    @DecimalMin(value = "0.01", message = "取款金额必须大于0")
    @DecimalMax(value = "50000", message = "单次取款金额不能超过5万")
    private BigDecimal amount;  // 取款金额

    @Size(max = 100, message = "备注长度不能超过100字")
    private String remark;  // 备注（可选）
}