package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.service.model.GenericResponse;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.OtpNotifMessage;
import com.auth.user.service.model.UserResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.url.shortener.config.WebSocketBrokerConfig.TOPIC_OTP;

@Slf4j
@AllArgsConstructor
@Service
public class OtpService {
    private static final int OTP_RANGE = 900000;
    private static final int OTP_BASE = 100000;

    private final OtpRepository otpRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserService userService;

    /**
     * Finds the user by phone number or registers a new user if not found.
     * then
     * Sends OTP to user but only if the previous OTP has expired.
     *
     * @param loginRequest the login request containing the phone number.
     **/
    public String sendOtp(LoginRequest loginRequest) {
        log.info("Received request to send OTP for user: {}", loginRequest.getPhoneNumber());
        GenericResponse<UserResponse> response = userService.findByPhoneNumberOrRegisterUser(
                loginRequest.getPhoneNumber()
        );

        if (response.hasError()) {
            return response.getError().getError();
        }

        User user = response.getData().getUser();

        // find active otp given user id, return string for the response
        return otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(user, LocalDateTime.now())
                .map(otp -> "Otp already sent! wait for 2 minute to generate new OTP")
                .orElseGet(() -> {
                    log.info("Generating OTP for user: {}", user.getPhoneNumber());
                    String otp = String.valueOf((int) (Math.random() * OTP_RANGE + OTP_BASE));
                    Otp otpEntity = Otp.builder().otp(otp).user(user).build();
                    otpRepository.save(otpEntity);

                    OtpNotifMessage otpMessage = OtpNotifMessage.builder()
                            .otp(otp)
                            .phoneNumber(user.getPhoneNumber())
                            .build();

                    log.info("Sending OTP {} for user: {}", otpMessage, user.getPhoneNumber());
                    simpMessagingTemplate.convertAndSend(TOPIC_OTP, otpMessage);

                    return "OTP sent successfully!";
                });
    }

    public Otp findOtpByPhoneNumber(User user) {
        return  otpRepository.findTop1ByUserAndUserActiveTrueAndExpiryTimeIsAfter(user, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("No OTP found for user: " + user.getPhoneNumber()));
    }
}
