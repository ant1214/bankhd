package com.zychen.bank.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class DepositDTO {

    @NotBlank(message = "银行卡号不能为空")
    private String cardId;  // 银行卡号

    @NotNull(message = "存款金额不能为空")
    @DecimalMin(value = "0.01", message = "存款金额必须大于0")
    @DecimalMax(value = "1000000", message = "单次存款金额不能超过100万")
    private BigDecimal amount;  // 存款金额
    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 6, message = "交易密码必须是6位数字")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须是6位数字")
    private String cardPassword;
    @Size(max = 100, message = "备注长度不能超过100字")
    private String remark;  // 备注（可选）
}