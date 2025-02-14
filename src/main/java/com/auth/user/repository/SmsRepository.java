package com.auth.user.repository;

import com.auth.user.entity.Sms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SmsRepository extends JpaRepository<Sms, Long> {

    long countAllByUserPhoneNumberAndActiveTrueAndCreatedAtBetween(
            String phoneNumber, LocalDateTime start, LocalDateTime end
    );
}
