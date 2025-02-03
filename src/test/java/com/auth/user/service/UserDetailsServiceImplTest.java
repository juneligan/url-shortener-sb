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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void whenLoadUserByUsername_UserFoundWithPassword() {
        // given
        User user = new User();
        user.setPhoneNumber("09123456789");
        user.setPassword("password");

        // when
        when(userRepository.findByPhoneNumberAndActiveTrue("09123456789")).thenReturn(Optional.of(user));

        // then
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername("09123456789");

        assertNotNull(userDetails);
        assertEquals("09123456789", userDetails.getPhoneNumber());
    }

    @Test
    public void whenLoadUserByUsername_UserNotFound() {
        // given
        String phoneNumber = "09123456789";

        // when
        when(userRepository.findByPhoneNumberAndActiveTrue(phoneNumber)).thenReturn(Optional.empty());

        // then
        assertThrows(
                UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(phoneNumber)
        );
    }

    @Test
    public void whenLoadUserByUsername_UserFoundWithoutPassword() {
        // given
        User user = new User();
        user.setPhoneNumber("09123456789");

        Otp otp = new Otp();
        otp.setOtp("123456");
        LocalDateTime now = LocalDateTime.now();
        otp.setExpiryTime(now.plusMinutes(5));

        // when
        when(userRepository.findByPhoneNumberAndActiveTrue("09123456789")).thenReturn(Optional.of(user));
        when(otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(eq(user), any(LocalDateTime.class)))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.encode("123456")).thenReturn("encodedOtp");

        // then
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername("09123456789");

        assertNotNull(userDetails);
        assertEquals("09123456789", userDetails.getPhoneNumber());
        assertEquals("encodedOtp", userDetails.getPassword());
    }

    @Test
    public void whenLoadUserByUsername_UserFoundWithoutPasswordAndOtpNotFound() {
        // given
        User user = new User();
        user.setPhoneNumber("09123456789");

        // when
        when(userRepository.findByPhoneNumberAndActiveTrue("09123456789")).thenReturn(Optional.of(user));
        when(otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(user, LocalDateTime.now()))
                .thenReturn(Optional.empty());

        // then
        assertThrows(RuntimeException.class, () -> userDetailsService.loadUserByUsername("09123456789"));
    }
}