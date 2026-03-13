package com.algorecall.notification;

import com.algorecall.model.Notification;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsNotificationService {

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.from-number:}")
    private String fromNumber;

    @Value("${app.twilio.enabled:false}")
    private boolean twilioEnabled;

    @PostConstruct
    public void init() {
        if (twilioEnabled && !accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized");
        } else {
            log.info("Twilio SMS service disabled (app.twilio.enabled=false or credentials missing)");
        }
    }

    public void send(Notification notification, String phoneNumber) {
        if (!twilioEnabled) {
            log.info("[SMS] Twilio disabled. SMS for user {}: {}",
                    notification.getUser().getUsername(), notification.getMessage());
            return;
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.info("[SMS] No phone number for user {}, skipping SMS",
                    notification.getUser().getUsername());
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromNumber),
                    notification.getMessage()
            ).create();

            log.info("[SMS] Sent to user {} (sid: {})",
                    notification.getUser().getUsername(), message.getSid());
        } catch (Exception e) {
            log.error("[SMS] Failed to send to user {}: {}",
                    notification.getUser().getUsername(), e.getMessage());
        }
    }
}
