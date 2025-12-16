package com.zychen.bank.model;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserInfo {
    private Long id;                // id
    private String userId;          // user_id
    private String name;            // name
    private String idNumber;        // id_number
    private Integer gender;         // gender: 0=女, 1=男
    private LocalDate birthDate;  // birth_date
    private String email;           // email
    private String address;         // address
    private LocalDateTime updatedTime; // updated_time
}
