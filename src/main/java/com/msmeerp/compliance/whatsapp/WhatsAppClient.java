package com.msmeerp.compliance.whatsapp;

/**
 * Sends WhatsApp messages. {@link LoggingWhatsAppClient} logs them in development; a provider client
 * (e.g. WhatsApp Business Cloud API with approved templates) only has to implement this.
 */
public interface WhatsAppClient {

    String channel();

    /** Sends {@code message} to an E.164 number such as +919822011223; throws on failure. */
    void send(String toE164, String message);
}
