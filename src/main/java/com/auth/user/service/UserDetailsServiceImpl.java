package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.service.model.UserDetailsImpl;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    /** Concrete class of UserDetailsService from Spring Security
     * This method loads a user by their phone number instead of username
     **/
    @Transactional
    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        // can't use userService because of circular dependency issue
        // i.e. webSecurityConfig > userService > userDetailsServiceImpl > webSecurityConfig
        String sanitizedPhoneNumber = UserService.getSanitizedPhoneNumber(phoneNumber);
        User user = userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User Not Found with phone number: " + sanitizedPhoneNumber)
                );

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            // can't use otpService because of circular dependency issue
            // i.e. webSecurityConfig  > otpService > userService > userDetailsServiceImpl > webSecurityConfig
            Otp otp = otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(user, LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("No OTP found for user: " + sanitizedPhoneNumber));
            // encode otp as temporary password for authentication
            return UserDetailsImpl.build(user, passwordEncoder.encode(otp.getOtp()));
        }

        return UserDetailsImpl.build(user);
    }
}
