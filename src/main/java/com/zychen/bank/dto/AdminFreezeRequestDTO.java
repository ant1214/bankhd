package com.zychen.bank.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminFreezeRequestDTO {

    @NotBlank(message = "目标类型不能为空")
    private String targetType; // account=账户冻结，card=银行卡冻结

    @NotBlank(message = "目标ID不能为空")
    private String targetId;   // 用户ID或卡号

    @NotBlank(message = "操作类型不能为空")
    private String operation;  // freeze=冻结，unfreeze=解冻

    @NotBlank(message = "原因类型不能为空")
    private String reasonType; // suspicious_activity=可疑交易, judicial=司法冻结, other=其他

    @NotBlank(message = "详细原因不能为空")
    private String reasonDetail;

    private Integer freezeDuration = 0; // 冻结天数，0=永久

    private Boolean notifyUser = true;
}