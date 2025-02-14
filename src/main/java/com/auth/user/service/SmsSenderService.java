package com.auth.user.service;

import com.auth.user.entity.Otp;
import com.auth.user.entity.Sms;
import com.auth.user.entity.User;
import com.auth.user.repository.OtpRepository;
import com.auth.user.repository.SmsRepository;
import com.auth.user.service.model.GenericResponse;
import com.auth.user.service.model.LoginRequest;
import com.auth.user.service.model.OtpNotifMessage;
import com.auth.user.service.model.SmsRequest;
import com.auth.user.service.model.UserResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.auth.user.exception.ErrorCode.SMS_LIMIT_REACHED;
import static com.auth.user.utils.UserUtils.DEFAULT_SMS_LIMIT_PER_HR;
import static com.url.shortener.config.WebSocketBrokerConfig.TOPIC_SMS;

@Slf4j
@Service
public class SmsSenderService {
    private final SmsRepository smsRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserService userService;

    @Autowired
    SmsSenderService(
            SmsRepository smsRepository,
            SimpMessagingTemplate simpMessagingTemplate,
            UserService userService
    ) {
        this.smsRepository = smsRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.userService = userService;
    }

    /**
     * Sends message to given phone number from authenticated user
     *
     * @param smsRequest the sms request containing the phone number.
     **/
    public GenericResponse<?> sendMessage(SmsRequest smsRequest, String username) {
        log.info("Received request to send sms for user: {}", username);
        GenericResponse<UserResponse> response = userService.findByPhoneNumberOrRegisterUser(username);

        if (response.hasError()) {
            return response;
        }

        User user = response.getData().getUser();
        LocalDateTime currentHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime nextHour = currentHour.plusHours(1);

        log.info("Checking sms limit for user: {}, between: {} - {}", user.getPhoneNumber(), currentHour, nextHour);

        long messagesSent = smsRepository.countAllByUserPhoneNumberAndActiveTrueAndCreatedAtBetween(
                user.getPhoneNumber(), currentHour, nextHour
        );

        int maxSmsPerHour = Objects.nonNull(user.getMaxSmsPerHour())
                ? user.getMaxSmsPerHour() : DEFAULT_SMS_LIMIT_PER_HR;

        if (messagesSent >= maxSmsPerHour) {
            return SMS_LIMIT_REACHED.toGenericResponse(user.getPhoneNumber());
        }

        Sms sms = Sms.builder()
                .user(user)
                .phoneNumber(smsRequest.getPhoneNumber())
                .message(smsRequest.getMessage())
                .build();

        smsRepository.save(sms);

        log.info("Sending sms to {} for user: {}", smsRequest.getPhoneNumber(), user.getPhoneNumber());
        simpMessagingTemplate.convertAndSend(TOPIC_SMS, smsRequest);

        return GenericResponse.builder().message("SMS sent successfully!").build();
    }
}
