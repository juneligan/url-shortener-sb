package com.auth.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attempt")
public class Attempt extends BaseEntity{
    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime lastAttempt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttemptType type; // OTP, SMS, not an enum
}