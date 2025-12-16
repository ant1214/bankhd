package com.zychen.bank.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FreezeCardDTO {
    @NotNull(message = "银行卡号不能为空")
    private String cardId;

    @NotNull(message = "交易密码不能为空")
    private String cardPassword;

    @NotNull(message = "冻结原因不能为空")
    private String reason; // 卡片遗失/信息泄露/其他

    private String contactPhone; // 联系电话
}
