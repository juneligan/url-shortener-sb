package com.auth.user.service;

import com.auth.user.entity.Attempt;
import com.auth.user.entity.AttemptType;
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
import com.auth.user.service.model.dbconfig.IDbConfig;
import com.auth.user.service.model.dbconfig.OtpAttemptConfig;
import com.auth.user.service.model.dbconfig.PasswordAttemptConfig;
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
import java.util.Optional;

import static com.auth.user.entity.AttemptType.VALIDATED_OTP;
import static com.auth.user.entity.DbConfigType.OTP_ATTEMPTS;
import static com.auth.user.entity.DbConfigType.PASSWORD_ATTEMPTS;
import static com.auth.user.exception.ErrorCode.FORBIDDEN;
import static com.auth.user.exception.ErrorCode.INVALID_OTP;
import static com.auth.user.exception.ErrorCode.OTP_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PASSWORD_ATTEMPTS_EXCEEDED;
import static com.auth.user.exception.ErrorCode.PHONE_NUMBER_IN_USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        IDbConfig config = new PasswordAttemptConfig();
        when(dbConfigService.getConfig(PASSWORD_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .lastAttempt(LocalDateTime.now().minusMinutes(1)) // Ensure lastAttempt is not null
                .build();
        when(attemptRepository.findByPhoneNumberAndType("1234567890", AttemptType.VALIDATED_PASSWORD))
                .thenReturn(Optional.of(attempt));

        when(userRepository.findByPhoneNumberAndActiveTrue("1234567890")).thenReturn(Optional.of(new User()));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        GenericResponse<JwtAuthenticationResponse> response = userService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getData()); // Ensure data is not null
        assertEquals("jwtToken", response.getData().getAccessToken());
    }

    @Test
    void testAuthenticateUser_UserNotFound() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber("1234567890")
                .password("password")
                .build();
        IDbConfig config = new PasswordAttemptConfig();
        when(dbConfigService.getConfig(PASSWORD_ATTEMPTS)).thenReturn(Optional.of(config));

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.empty());

        GenericResponse<JwtAuthenticationResponse> response = userService.authenticateUser(loginRequest);

        assertTrue(response.hasError());
        assertEquals(FORBIDDEN.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testAuthenticateUser_Login_PasswordAttemptsExceeded() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber("1234567890")
                .password("password")
                .build();

        when(userRepository.findByPhoneNumberAndActiveTrue("1234567890")).thenReturn(Optional.of(new User()));

        PasswordAttemptConfig config = new PasswordAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(PASSWORD_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(3)
                .lastAttempt(LocalDateTime.now())
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.of(attempt));

        GenericResponse<JwtAuthenticationResponse> response = userService.authenticateUser(loginRequest);

        assertTrue(response.hasError());
        assertEquals(PASSWORD_ATTEMPTS_EXCEEDED.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testAuthenticateUser_Login_ResetPasswordAttempts() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber("1234567890")
                .password("password")
                .build();

        PasswordAttemptConfig config = new PasswordAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(PASSWORD_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(2)
                .lastAttempt(LocalDateTime.now().minusMinutes(3)) // Ensure lastAttempt is before reset time
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.of(attempt));
        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(new User()));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        GenericResponse<JwtAuthenticationResponse> response = userService.authenticateUser(loginRequest);

        assertFalse(response.hasError());
        assertNotNull(response.getData());
        assertEquals("jwtToken", response.getData().getAccessToken());
        assertEquals(0, attempt.getAttempts()); // Ensure attempts are reset
    }

    @Test
    void testAuthenticateUser_InvalidOtp() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("123456")
                .build();

        IDbConfig config = new OtpAttemptConfig();
        when(dbConfigService.getConfig(OTP_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .lastAttempt(LocalDateTime.now().minusMinutes(1)) // Ensure lastAttempt is not null
                .build();
        when(attemptRepository.findByPhoneNumberAndType("1234567890", VALIDATED_OTP))
                .thenReturn(Optional.of(attempt));

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

        OtpAttemptConfig config = new OtpAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(OTP_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(3)
                .lastAttempt(LocalDateTime.now())
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.of(attempt));

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

        OtpAttemptConfig config = new OtpAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(OTP_ATTEMPTS)).thenReturn(Optional.of(config));
        when(userRepository.findByPhoneNumberAndActiveTrue("1234567890")).thenReturn(Optional.of(new User()));
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.empty());
        Otp otp = Otp.builder()
                .otp("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .user(new User())
                .verified(false)
                .build();
        when(otpRepository.findTop1ByOtpAndExpiryTimeAfterAndVerifiedIsFalseAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                anyString(), any(LocalDateTime.class), anyString())).thenReturn(Optional.of(otp));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertFalse(response.hasError());
        assertNotNull(response.getData());
        assertInstanceOf(JwtAuthenticationResponse.class, response.getData());
        assertEquals("jwtToken", ((JwtAuthenticationResponse) response.getData()).getAccessToken());
    }

    @Test
    void testAuthenticateUser_Otp_OtpAttemptsExceeded() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("123456")
                .build();

        when(userRepository.findByPhoneNumberAndActiveTrue("1234567890")).thenReturn(Optional.of(new User()));

        OtpAttemptConfig config = new OtpAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(OTP_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(3)
                .lastAttempt(LocalDateTime.now())
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.of(attempt));

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertTrue(response.hasError());
        assertEquals(OTP_ATTEMPTS_EXCEEDED.getCode(), response.getError().getErrorCode());
    }

    @Test
    void testAuthenticateUser_OtpLogin_ResetPasswordAttempts() {
        LoginRequest loginRequest = LoginRequest.builder()
                .phoneNumber("1234567890")
                .password("password")
                .build();

        PasswordAttemptConfig config = new PasswordAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(PASSWORD_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(2)
                .lastAttempt(LocalDateTime.now().minusMinutes(3)) // Ensure lastAttempt is before reset time
                .build();
        when(attemptRepository.findByPhoneNumberAndType(anyString(), any())).thenReturn(Optional.of(attempt));
        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(new User()));

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(UserDetailsImpl.build(new User()));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("jwtToken");

        GenericResponse<JwtAuthenticationResponse> response = userService.authenticateUser(loginRequest);

        assertFalse(response.hasError());
        assertNotNull(response.getData());
        assertEquals("jwtToken", response.getData().getAccessToken());
        assertEquals(0, attempt.getAttempts()); // Ensure attempts are reset
    }

    @Test
    void testAuthenticateUser_Otp_EmptyOtp() {
        OtpRequest otpRequest = OtpRequest.builder()
                .phoneNumber("1234567890")
                .otp("")
                .build();

        OtpAttemptConfig config = new OtpAttemptConfig();
        config.setMaxAttempts(3);
        config.setResetMinutes(2);

        when(dbConfigService.getConfig(OTP_ATTEMPTS)).thenReturn(Optional.of(config));
        Attempt attempt = Attempt.builder()
                .attempts(0)
                .lastAttempt(LocalDateTime.now().minusMinutes(1)) // Ensure lastAttempt is not null
                .build();
        when(attemptRepository.findByPhoneNumberAndType("1234567890", VALIDATED_OTP))
                .thenReturn(Optional.of(attempt));

        GenericResponse<?> response = userService.authenticateUser(otpRequest);

        assertTrue(response.hasError());
        assertEquals(INVALID_OTP.getCode(), response.getError().getErrorCode());
    }
}