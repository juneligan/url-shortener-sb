package com.url.shortener.service;

import com.url.shortener.service.model.NotificationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


//@Service
//@RequiredArgsConstructor
public class WebSocketMessageService { // Test class to send websocket message to client

//    private final SimpMessagingTemplate simpMessagingTemplate;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

//    @PostConstruct
//    public void startSendingMessages() {
//        scheduler.scheduleAtFixedRate(this::sendMessage, 0, 5, TimeUnit.SECONDS);
//    }

    private void sendMessage() {
        NotificationMessage message = NotificationMessage.builder()
                .otp("123124")
                .text("Scheduled message")
                .phoneNumber("09912098012")
                .to("specificUser")
                .build();

        System.out.println("message = " + message);
//        simpMessagingTemplate.convertAndSend("/notification", message);
    }
}