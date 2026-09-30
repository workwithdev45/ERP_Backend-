package com.msmeerp.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReminderDto {
    private Long id;
    private Long documentId;
    private String documentNumber;
    private String partyName;
    private String channel;
    private String recipient;
    private String triggerType;
    private String status;
    private String message;
    private String error;
    private Instant sentAt;
}
