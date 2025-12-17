package com.zychen.bank.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetUserPasswordDTO {
    @NotBlank(message = "目标用户ID不能为空")
    private String targetUserId;

    private String reason;  // 重置原因（可选）
}