package com.auth.user.service;

import com.auth.user.entity.Attempt;
import com.auth.user.entity.Otp;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.auth.user.exception.ErrorCode.INVALID_OTP;
import static com.auth.user.exception.ErrorCode.OTP_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PHONE_NUMBER_IN_USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private OtpRepository otpRepository;

    @Mock
    private DbConfigService dbConfigService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_UserExists() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .phoneNumber("1234567890")
                .build();

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(new User()));

        GenericResponse<UserResponse> response = userService.registerUser(registerRequest);

        assertTrue(response.hasError());
        assertEquals(PHONE_NUMBER_IN_USE.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testRegisterUser_Success() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .phoneNumber("1234567890")
                .email("test@example.com")
                .username("testuser")
                .password("password")
                .build();

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        GenericResponse<UserResponse> response = userService.registerUser(registerRequest);

        assertFalse(response.hasError());
        assertNotNull(response.getData());
    }

    @Test
    void testAuthenticateUser_Success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber("1234567890")
                .password("password")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        JwtAuthenticationResponse response = userService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
    }


    @Test
    void testAuthenticateUser_InvalidOtp() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("123456")
                .build();

        when(dbConfigService.getConfig(anyString())).thenReturn(Optional.of(Map.of("maxAttempts", 3, "resetMinutes", 2)));
        when(attemptRepository.findByPhoneNumberAndType(anyString(), anyString())).thenReturn(Optional.of(new Attempt()));

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertTrue(response.hasError());
        assertEquals(INVALID_OTP.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testAuthenticateUser_OtpAttemptsExceeded() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("123456")
                .build();

        when(dbConfigService.getConfig(anyString())).thenReturn(Optional.of(Map.of("maxAttempts", 3, "resetMinutes", 2)));
        Attempt attempt = Attempt.builder()
                .attempts(3)
                .lastAttempt(LocalDateTime.now())
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), anyString())).thenReturn(Optional.of(attempt));

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertTrue(response.hasError());
        assertEquals(OTP_ATTEMPTS_EXCEEDED.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testAuthenticateUserForOtpLogin_Success() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("123456")
                .build();

        when(dbConfigService.getConfig(anyString())).thenReturn(Optional.of(Map.of("maxAttempts", 3, "resetMinutes", 2)));
        when(attemptRepository.findByPhoneNumberAndType(anyString(), anyString())).thenReturn(Optional.empty());
        Otp otp = Otp.builder()
                .otp("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .user(new User())
                .verified(false)
                .build();
        when(otpRepository.findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                anyString(), any(LocalDateTime.class), anyString())).thenReturn(Optional.of(otp));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertFalse(response.hasError());
        assertNotNull(response.getData());
        assertTrue(response.getData() instanceof JwtAuthenticationResponse);
        assertEquals("jwtToken", ((JwtAuthenticationResponse) response.getData()).getAccessToken());
    }
}