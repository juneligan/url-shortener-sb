package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.service.model.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    public void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userRepository, otpRepository, passwordEncoder);
    }

    @Test
    public void testLoadUserByUsername_UserFoundWithPassword() {
        User user = new User();
        user.setPhoneNumber("09123456789");
        user.setPassword("password");

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(user));

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername("09123456789");

        assertNotNull(userDetails);
        assertEquals("09123456789", userDetails.getUsername());
    }

    @Test
    public void testLoadUserByUsername_UserNotFound() {
        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("09123456789");
        });
    }

    @Test
    public void testLoadUserByUsername_UserFoundWithoutPassword() {
        User user = new User();
        user.setPhoneNumber("09123456789");

        Otp otp = new Otp();
        otp.setOtp("123456");
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(user));
        when(otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(any(User.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedOtp");

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername("09123456789");

        assertNotNull(userDetails);
        assertEquals("09123456789", userDetails.getUsername());
        assertEquals("encodedOtp", userDetails.getPassword());
    }

    @Test
    public void testLoadUserByUsername_UserFoundWithoutPasswordAndOtpNotFound() {
        User user = new User();
        user.setPhoneNumber("09123456789");

        when(userRepository.findByPhoneNumberAndActiveTrue(anyString())).thenReturn(Optional.of(user));
        when(otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(any(User.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            userDetailsService.loadUserByUsername("09123456789");
        });
    }
}