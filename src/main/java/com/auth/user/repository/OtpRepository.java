package com.auth.user.repository;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
            String otp, LocalDateTime expiryTimeAfter, String userPhoneNumber
    );
    Optional<Otp> findTop1ByUserAndExpiryTimeIsAfter(User user, LocalDateTime expiryTimeBefore);
}
