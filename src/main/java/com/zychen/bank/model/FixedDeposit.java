package com.zychen.bank.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FixedDeposit {
    private Integer fdId;           // 对应 fd_id
    private String cardId;          // 对应 card_id
    private String userId;          // 对应 user_id
    private BigDecimal principal;   // 对应 principal
    private BigDecimal rate;        // 对应 annual_rate
    private Integer term;           // 对应 term_months
    private Date startTime;         // 对应 start_date
    private Date endTime;           // 对应 end_date
    private Boolean autoRenew;      // 对应 auto_renew
    private Integer status;         // 对应 status
    private Date createdTime;       // 对应 created_time
    private String fdNo;
}