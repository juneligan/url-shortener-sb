package com.auth.user.repository;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByOtpAndExpiryTimeAfterAndUser_PhoneNumber(String otp, LocalDateTime expiryTimeBefore, String userPhoneNumber);

    Optional<Otp> findTop1ByUserAndExpiryTimeIsAfter(User user, LocalDateTime expiryTimeBefore);

    Optional<Otp> findTop1ByUser_PhoneNumberAndExpiryTimeIsBefore(String phoneNumber, LocalDateTime expiryTimeBefore);
}
