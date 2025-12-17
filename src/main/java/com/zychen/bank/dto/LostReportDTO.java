package com.zychen.bank.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LostReportDTO {

    @NotBlank(message = "卡号不能为空")
    private String cardId;

    @NotBlank(message = "操作类型不能为空")
    private String operation; // report=挂失，cancel=解挂

    @NotBlank(message = "原因类型不能为空")
    private String reasonType; // card_lost=卡片遗失，card_damaged=卡片损坏，other=其他

    @NotBlank(message = "详细原因不能为空")
    private String reasonDetail;

    private Boolean notifyUser = true;
}
