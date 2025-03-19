package com.auth.user.repository;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    // for logging in and before authenticating creds
    Optional<Otp> findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
            String otp, LocalDateTime expiryTimeAfter, String userPhoneNumber
    );

    // before sending otp
    Optional<Otp> findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(User user, LocalDateTime expiryTimeBefore);

    // for authentication
    Optional<Otp> findTop1ByUserAndUserActiveTrueAndVerifiedTrueAndActiveTrueAndDeletedFalseOrderByIdDesc(User user);
}
