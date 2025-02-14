package com.auth.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.auth.user.utils.UserUtils.DEFAULT_SMS_LIMIT_PER_HR;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "users")
public class User extends BaseEntity {
    @Column(name = "phone_number", unique=true, nullable = false)
    private String phoneNumber; // unique
    private String email;
    private String username;
    private String password;
    private String role = "ROLE_USER";
    @Column(nullable = false)
    private Boolean phoneNumberVerified = false;
    @Column(nullable = false)
    private Boolean emailVerified = false;
    private Integer maxSmsPerHour = DEFAULT_SMS_LIMIT_PER_HR;
}
