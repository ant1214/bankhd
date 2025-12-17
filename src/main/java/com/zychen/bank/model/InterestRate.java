package com.zychen.bank.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class InterestRate {
    private Long rateId;
    private String accountType;      // CURRENT=活期，FIXED_3M=定期3月...
    private Integer termMonths;      // 期限（月），活期为0
    private BigDecimal annualRate;   // 年利率
    private BigDecimal dailyRate;    // 日利率（计算列）
    private Date effectiveDate;      // 生效日期
    private Boolean isActive;        // 是否有效
    private String createdBy;
    private Date createdTime;
}