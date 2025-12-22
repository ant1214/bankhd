package com.zychen.bank.dto;


import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminTransactionQueryDTO {
    private Integer page = 1;                // 页码
    private Integer pageSize = 20;           // 每页数量
    private String userId;                   // 用户ID筛选
    private String cardId;                   // 银行卡号筛选
    private String transType;               // 交易类型：DEPOSIT/WITHDRAW/TRANSFER/INTEREST
    private String transNo;                  // 交易流水号
    private Integer status;                  // 状态：0=失败，1=成功，2=处理中
    private LocalDate startDate;             // 开始日期
    private LocalDate endDate;               // 结束日期
    private String userName;                 // 用户名筛选（需要关联查询）
}