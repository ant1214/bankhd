package com.zychen.bank.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FreezeRecord {
    private Long recordId;
    private String freezeNo;       // 冻结编号
    private Integer freezeType;    // 冻结类型：1=银行卡冻结，2=账户冻结
    private Integer freezeLevel;   // 冻结级别：1=用户申请，2=管理员操作，3=系统风控
    private String targetId;       // 目标ID（user_id或card_id）
    private String userId;         // 用户ID
    private String cardId;         // 银行卡号
    private String reasonType;     // 原因类型
    private String reasonDetail;   // 详细原因
    private LocalDateTime freezeTime;      // 冻结时间
    private LocalDateTime unfreezeTime;    // 解冻时间
    private LocalDateTime plannedUnfreezeTime; // 计划解冻时间
    private String operatorId;     // 操作员ID
    private Integer operatorRole;  // 操作员角色
    private Integer status;        // 状态：1=冻结中，0=已解冻，2=已过期
}