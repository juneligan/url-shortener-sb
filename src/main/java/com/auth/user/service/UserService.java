package com.auth.user.service;

import com.auth.user.entity.AttemptType;
import com.auth.user.entity.Otp;
import com.auth.user.entity.Attempt;
import com.auth.user.entity.User;
import com.auth.user.repository.AttemptRepository;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.security.JwtAuthenticationResponse;
import com.auth.user.security.JwtUtils;
import com.auth.user.service.model.GenericResponse;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.OtpRequest;
import com.auth.user.service.model.RegisterRequest;
import com.auth.user.service.model.UserDetailsImpl;
import com.auth.user.service.model.UserResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.auth.user.entity.AttemptType.VALIDATED_OTP;
import static com.auth.user.exception.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.auth.user.exception.ErrorCode.INVALID_OTP;
import static com.auth.user.exception.ErrorCode.OTP_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PHONE_NUMBER_IN_USE;
import static com.auth.user.utils.UserUtils.getSanitizedPhoneNumber;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {
    public static final String DEFAULT_ROLE_USER = "ROLE_USER";
    private static final String OTP_ATTEMPTS = "OTP_ATTEMPTS";
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AttemptRepository attemptRepository;
    private final OtpRepository otpRepository;
    private final DbConfigService dbConfigService;

    public GenericResponse<UserResponse> registerUser(RegisterRequest registerRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(registerRequest.getPhoneNumber());
        Optional<User> existingUser = userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber);

        if (existingUser.isPresent()) {
            return GenericResponse.<UserResponse>builder()
                    .error(PHONE_NUMBER_IN_USE.toErrorResponse(sanitizedPhoneNumber))
                    .build();
        }
        User user = new User();
        user.setPhoneNumber(sanitizedPhoneNumber);
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setRole(DEFAULT_ROLE_USER); // static for now
        if (registerRequest.getPassword() != null) { // password is optional for otp typed users
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        }

        return GenericResponse.<UserResponse>builder()
                .data(UserResponse.build(userRepository.save(user)))
                .build();
    }

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        return getJwtAuthenticationResponse(loginRequest.getPhoneNumber(), loginRequest.getPassword());
    }

    public GenericResponse<?> authenticateUser(OtpRequest otpRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(otpRequest.getPhoneNumber());
        // Check OTP attempts

        GenericResponse<?> configResult = findOtpConfig(sanitizedPhoneNumber);
        if (configResult.hasError()) {
            return configResult;
        }
        Map<String, Object> config = (Map<String, Object>) configResult.getData();
        int maxAttempts = (int) config.get("maxAttempts");
        int resetMinutes = (int) config.get("resetMinutes");

        Optional<Attempt> otpAttemptOpt = attemptRepository.findByPhoneNumberAndType(sanitizedPhoneNumber, VALIDATED_OTP);
        Attempt attempt = otpAttemptOpt
                .orElseGet(() -> Attempt.builder()
                        .phoneNumber(sanitizedPhoneNumber)
                        .attempts(0)
                        .type(VALIDATED_OTP)
                        .lastAttempt(LocalDateTime.now())
                        .build()
                );

        if (attempt.getAttempts() >= maxAttempts
                && attempt.getLastAttempt().isAfter(LocalDateTime.now().minusMinutes(resetMinutes))) {
            return OTP_ATTEMPTS_EXCEEDED.toGenericResponse(sanitizedPhoneNumber);
        }

        Optional<Otp> otp = findActiveOtp(otpRequest.getOtp(), sanitizedPhoneNumber);

        // invalid if otp is not linked to the phone number
        // invalid if the otp is expired
        // invalid if the otp is not found
        if (otp.isEmpty()) {
            attempt.setAttempts(attempt.getAttempts() + 1);
            attempt.setLastAttempt(LocalDateTime.now());
            attemptRepository.save(attempt);
            return INVALID_OTP.toGenericResponse(sanitizedPhoneNumber);
        }

        Otp otpEntity = otp.get();
        updatePhoneNumberVerification(otpEntity);
        JwtAuthenticationResponse jwtAuthenticationResponse = getJwtAuthenticationResponse(
                otpRequest.getPhoneNumber(), otpEntity.getOtp()
        );

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);
        return GenericResponse.builder().data(jwtAuthenticationResponse).build();
    }

    // this will be used for OTP login since the user will not go through registration process
    public GenericResponse<UserResponse> findByPhoneNumberOrRegisterUser(String phoneNumber) {
        // sanitize phone number to remove country code
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(phoneNumber);
        return userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .map(user -> GenericResponse.<UserResponse>builder().data(UserResponse.build(user)).build())
                .orElseGet(() -> {
                    RegisterRequest registerRequest = RegisterRequest.builder()
                            .phoneNumber(sanitizedPhoneNumber)
                            .build();
                    return registerUser(registerRequest);
                });
    }

    public User findByPhoneNumber(String phoneNumber) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(phoneNumber);
        return userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found with phone number: " + phoneNumber));
    }

    private JwtAuthenticationResponse getJwtAuthenticationResponse(String username, String password) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(username);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(sanitizedPhoneNumber, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }

    private void updatePhoneNumberVerification(Otp otpEntity) {
        User user = otpEntity.getUser();
        if (!user.getPhoneNumberVerified()) {
            user.setPhoneNumberVerified(true);
            userRepository.save(user);
        }
    }

    private GenericResponse<?> findOtpConfig(String phoneNumber) {
        Optional<Map<String, Object>> optConfig = dbConfigService.getConfig(OTP_ATTEMPTS);

        if (optConfig.isEmpty()) {
            log.error("OTP_ATTEMPTS config not found");
            return INTERNAL_SERVER_ERROR.toGenericResponse(phoneNumber);
        }

        return GenericResponse.builder().data(optConfig.get()).build();
    }

    private Optional<Otp> findActiveOtp(String otp, String phoneNumber) {
        return otpRepository.findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                otp, LocalDateTime.now(), phoneNumber
        );
    }
}
