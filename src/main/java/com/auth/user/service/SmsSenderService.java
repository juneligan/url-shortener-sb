package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.OtpNotifMessage;
import com.auth.user.service.model.SmsRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.url.shortener.config.WebSocketBrokerConfig.TOPIC_SMS;

@Slf4j
@AllArgsConstructor
@Service
public class SmsSenderService {
    private final OtpRepository otpRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserService userService;

    /**
     * Sends message to given phone number from authenticated user
     *
     * @param smsRequest the sms request containing the phone number.
     **/
    public String sendMessage(SmsRequest smsRequest, String username) {
        log.info("Received request to send sms for user: {}", username);
        User user = userService.findByPhoneNumberOrRegisterUser(username);

        log.info("Sending sms to {} for user: {}", smsRequest.getPhoneNumber(), user.getPhoneNumber());
        simpMessagingTemplate.convertAndSend(TOPIC_SMS, smsRequest);

        return "SMS sent successfully!";
    }
}
