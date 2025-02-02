package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.exception.ErrorResponse;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.UserRepository;
import com.auth.user.security.JwtAuthenticationResponse;
import com.auth.user.security.JwtUtils;
import com.auth.user.service.model.OtpRequest;
import com.auth.user.service.model.SbResponse;
import com.auth.user.service.model.UserDetailsImpl;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.RegisterRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserService {
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private OtpRepository otpRepository;

    public User registerUser(RegisterRequest registerRequest) {
        userRepository.findByPhoneNumber(registerRequest.getPhoneNumber())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Phone number already in use!");
                });
        User user = new User();
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setRole("ROLE_USER"); // static for now
        if (registerRequest.getPassword() != null) { // password is optional for otp typed users
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        }

        return userRepository.save(user);
    }

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        return getJwtAuthenticationResponse(loginRequest.getPhoneNumber(), loginRequest.getPassword());
    }

    public SbResponse authenticateUser(OtpRequest otpRequest) {
        Optional<Otp> otp = otpRepository.findTop1ByOtpAndExpiryTimeAfterAndUserPhoneNumberAndUserPasswordIsNullAndUserActiveIsTrue(
                otpRequest.getOtp(),
                LocalDateTime.now(),
                otpRequest.getPhoneNumber()
        );

        // invalid if otp is not linked to the phone number
        // invalid if the otp is expired
        // invalid if the otp is not found
        if (otp.isEmpty()) {
            return ErrorResponse.builder().error("Invalid OTP! not found or expired").build();
        }

        return getJwtAuthenticationResponse(otpRequest.getPhoneNumber(), otp.get().getOtp());
    }

    // this will be used for OTP login since the user will not go through registration process
    public User findByPhoneNumberOrRegisterUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    RegisterRequest registerRequest = RegisterRequest.builder().phoneNumber(phoneNumber).build();
                    return registerUser(registerRequest);
                });
    }

    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found with phone number: " + phoneNumber));
    }

    private JwtAuthenticationResponse getJwtAuthenticationResponse(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }
}
