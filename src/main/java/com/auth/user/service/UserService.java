package com.auth.user.service;

import com.auth.user.entity.Attempt;
import com.auth.user.entity.AttemptType;
import com.auth.user.entity.DbConfigType;
import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.exception.ErrorResponse;
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
import com.auth.user.service.model.dbconfig.IAttemptConfig;
import com.auth.user.service.model.dbconfig.IDbConfig;
import com.auth.user.service.model.dbconfig.OtpAttemptConfig;
import com.auth.user.service.model.dbconfig.PasswordAttemptConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.auth.user.entity.AttemptType.VALIDATED_OTP;
import static com.auth.user.entity.AttemptType.VALIDATED_PASSWORD;
import static com.auth.user.entity.DbConfigType.OTP_ATTEMPTS;
import static com.auth.user.entity.DbConfigType.PASSWORD_ATTEMPTS;
import static com.auth.user.exception.ErrorCode.FORBIDDEN;
import static com.auth.user.exception.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.auth.user.exception.ErrorCode.INVALID_OTP;
import static com.auth.user.exception.ErrorCode.OTP_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PASSWORD_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PHONE_NUMBER_IN_USE;
import static com.auth.user.utils.UserUtils.getSanitizedPhoneNumber;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {
    public static final String DEFAULT_ROLE_USER = "ROLE_USER";
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

    public GenericResponse<JwtAuthenticationResponse> authenticateUser(LoginRequest loginRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(loginRequest.getPhoneNumber());
        GenericResponse<?> configResult = findConfig(sanitizedPhoneNumber, PASSWORD_ATTEMPTS);
        if (configResult.hasError()) {
            return GenericResponse.<JwtAuthenticationResponse>builder()
                    .error(configResult.getError())
                    .build();
        }
        Optional<User> existingUser = userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber);
        if (existingUser.isEmpty()) {
            log.warn("Login: User not found with phone number: {}", sanitizedPhoneNumber);
            return GenericResponse.<JwtAuthenticationResponse>builder()
                    .error(ErrorResponse.build(FORBIDDEN, sanitizedPhoneNumber))
                    .build();
        }

        PasswordAttemptConfig config = (PasswordAttemptConfig) configResult.getData();
        Attempt attempt = getAttempt(sanitizedPhoneNumber, VALIDATED_PASSWORD);
        if (isInvalidAttempt(attempt, config)) {
            return PASSWORD_ATTEMPTS_EXCEEDED.toGenericResponse(sanitizedPhoneNumber, config.getResetMinutes());
        } else if (attempt.getLastAttempt().isBefore(LocalDateTime.now().minusMinutes(config.getResetMinutes()))) {
            attempt.setAttempts(0);
        }
        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastAttempt(LocalDateTime.now());
        attemptRepository.save(attempt);

        GenericResponse<JwtAuthenticationResponse> response = GenericResponse.<JwtAuthenticationResponse>builder()
                .data(getJwtAuthenticationResponse(sanitizedPhoneNumber, loginRequest.getPassword()))
                .build();

        // Reset the attempt count after successful password validation
        attempt.setAttempts(0);
        attempt.setLastAttempt(LocalDateTime.now());
        attemptRepository.save(attempt);

        return response;
    }

    public GenericResponse<?> authenticateUser(OtpRequest otpRequest) {
        String sanitizedPhoneNumber = getSanitizedPhoneNumber(otpRequest.getPhoneNumber());

        GenericResponse<?> configResult = findConfig(sanitizedPhoneNumber, OTP_ATTEMPTS);
        if (configResult.hasError()) {
            return configResult;
        }
        OtpAttemptConfig config = (OtpAttemptConfig) configResult.getData();
        Attempt attempt = getAttempt(sanitizedPhoneNumber, VALIDATED_OTP);

        if (isInvalidAttempt(attempt, config)) {
            return OTP_ATTEMPTS_EXCEEDED.toGenericResponse(sanitizedPhoneNumber, config.getResetMinutes());
        } else if (attempt.getLastAttempt().isBefore(LocalDateTime.now().minusMinutes(config.getResetMinutes()))) {
            attempt.setAttempts(0);
        }

        Optional<User> existingUser = userRepository.findByPhoneNumberAndActiveTrue(sanitizedPhoneNumber);
        if (existingUser.isEmpty()) {
            log.warn("OTP Login: User not found with phone number: {}", sanitizedPhoneNumber);
            // use the same error message as invalid OTP to avoid leaking user information
            return INVALID_OTP.toGenericResponse(sanitizedPhoneNumber, config.getResetMinutes());
        }

        Optional<Otp> otp = findActiveOtp(otpRequest.getOtp(), sanitizedPhoneNumber);

        // invalid if otp is not linked to the phone number
        // invalid if the otp is expired
        // invalid if the otp is not found
        if (otp.isEmpty()) {
            attempt.setAttempts(attempt.getAttempts() + 1);
            attempt.setLastAttempt(LocalDateTime.now());
            attemptRepository.save(attempt);
            return INVALID_OTP.toGenericResponse(sanitizedPhoneNumber, config.getResetMinutes());
        }

        Otp otpEntity = otp.get();
        updatePhoneNumberVerification(otpEntity);
        JwtAuthenticationResponse jwtAuthenticationResponse = getJwtAuthenticationResponse(
                otpRequest.getPhoneNumber(), otpEntity.getOtp()
        );

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);

        // Reset the attempt count after successful OTP validation
        attempt.setAttempts(0);
        attempt.setLastAttempt(LocalDateTime.now());
        attemptRepository.save(attempt);

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

    private JwtAuthenticationResponse getJwtAuthenticationResponse(String sanitizedPhoneNumber, String password) {
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

    private GenericResponse<? extends IDbConfig> findConfig(String phoneNumber, DbConfigType configName) {
        Optional<? extends IDbConfig> optConfig = dbConfigService.getConfig(configName);

        if (optConfig.isEmpty()) {
            log.error("{} config not found", configName);
            return INTERNAL_SERVER_ERROR.toGenericResponse(phoneNumber);
        }

        return GenericResponse.<IDbConfig>builder().data(optConfig.get()).build();
    }

    private Optional<Otp> findActiveOtp(String otp, String phoneNumber) {
        return otpRepository.findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                otp, LocalDateTime.now(), phoneNumber
        );
    }

    private static boolean isInvalidAttempt(Attempt attempt, IAttemptConfig config) {
        return attempt.getAttempts() >= config.getMaxAttempts()
                && attempt.getLastAttempt().isAfter(LocalDateTime.now().minusMinutes(config.getResetMinutes()));
    }

    private Attempt getAttempt(String sanitizedPhoneNumber, AttemptType type) {
        return attemptRepository.findByPhoneNumberAndType(sanitizedPhoneNumber, type)
                .orElseGet(() -> Attempt.builder()
                        .phoneNumber(sanitizedPhoneNumber)
                        .attempts(0)
                        .type(type)
                        .lastAttempt(LocalDateTime.now())
                        .build()
                );
    }
}
