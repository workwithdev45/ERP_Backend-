package com.msmeerp.compliance.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Development stand-in: logs the message instead of sending it, like email's dev mode. */
@Component
@Slf4j
public class LoggingWhatsAppClient implements WhatsAppClient {

    @Override
    public String channel() {
        return "WHATSAPP";
    }

    @Override
    public void send(String toE164, String message) {
        log.info("\n======================== [WHATSAPP DEV MODE] ========================\nTo:      {}\nMessage: {}\n=====================================================================",
                toE164, message);
    }
}
