package com.auth.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "phone_number", unique=true, nullable = false)
    private String phoneNumber; // unique
    private String email;
    private String username;
    private String password;
    private String role = "ROLE_USER";
    private Boolean phoneNumberVerified = false;
    private Boolean emailVerified = false;
    private Boolean active = true;
}
