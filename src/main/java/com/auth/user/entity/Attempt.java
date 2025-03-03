package com.auth.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attempt")
public class Attempt extends BaseEntity{

    // can't add user because the user entity is not present if the login/auth is failed
//    @ManyToOne(fetch = FetchType.EAGER)
//    @Fetch(FetchMode.JOIN)
//    @JoinColumn(name = "user_id", referencedColumnName = "id", updatable = false, nullable = false)
//    private User user;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime lastAttempt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttemptType type; // OTP, SMS, not an enum

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "phone_number", referencedColumnName = "phone_number", insertable = false, updatable = false)
    private User user;
}