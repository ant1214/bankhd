package com.zychen.bank.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private String userId;           // user_id
    private String username;         // username
    private String phone;            // phone
    private String password;         // password
    private Integer role;            // role: 0=用户, 1=管理员
    private Integer accountStatus;   // account_status: 0=正常, 1=冻结
    private LocalDateTime createdTime;   // created_time
    private LocalDateTime lastLoginTime; // last_login_time
}
