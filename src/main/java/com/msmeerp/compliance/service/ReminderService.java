package com.msmeerp.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msmeerp.common.exception.BadRequestException;
import com.msmeerp.common.exception.ResourceNotFoundException;
import com.msmeerp.common.util.SecurityUtils;
import com.msmeerp.compliance.dto.PaymentReminderDto;
import com.msmeerp.compliance.dto.ReminderSettingsDto;
import com.msmeerp.compliance.entity.PaymentReminder;
import com.msmeerp.compliance.repository.PaymentReminderRepository;
import com.msmeerp.compliance.whatsapp.WhatsAppClient;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.tenant.entity.TenantSettings;
import com.msmeerp.tenant.repository.TenantRepository;
import com.msmeerp.tenant.repository.TenantSettingsRepository;
import com.msmeerp.trade.entity.DocumentStatus;
import com.msmeerp.trade.entity.DocumentType;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.TradeDocument;
import com.msmeerp.trade.repository.PartyRepository;
import com.msmeerp.trade.repository.TradeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** W13: WhatsApp payment reminders for unpaid invoices — on demand and on a daily schedule. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    static final String SETTINGS_KEY = "sales.paymentReminders";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final WhatsAppClient whatsApp;
    private final PaymentReminderRepository reminderRepository;
    private final TradeDocumentRepository documentRepository;
    private final PartyRepository partyRepository;
    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository settingsRepository;
    private final ObjectMapper objectMapper;

    // -- settings ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReminderSettingsDto getSettings() {
        return settingsRepository.findByTenantIdAndKey(TenantContext.getTenantId(), SETTINGS_KEY)
                .map(s -> fromJson(s.getValue()))
                .orElseGet(() -> ReminderSettingsDto.builder().build());
    }

    @Transactional
    public ReminderSettingsDto updateSettings(ReminderSettingsDto settings) {
        String tenantId = TenantContext.getTenantId();
        TenantSettings row = settingsRepository.findByTenantIdAndKey(tenantId, SETTINGS_KEY)
                .orElseGet(() -> TenantSettings.builder().tenant(tenantRepository.getReferenceById(tenantId)).key(SETTINGS_KEY).build());
        row.setValue(toJson(settings));
        settingsRepository.save(row);
        return settings;
    }

    // -- sending -------------------------------------------------------------------------------

    @Transactional
    public PaymentReminderDto sendNow(Long invoiceId) {
        TradeDocument invoice = documentRepository.findByTenantIdAndId(TenantContext.getTenantId(), invoiceId)
                .filter(d -> d.getDocType() == DocumentType.SALES_INVOICE)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        if (invoice.getStatus() != DocumentStatus.UNPAID && invoice.getStatus() != DocumentStatus.PARTIALLY_PAID) {
            throw new BadRequestException(invoice.getDocNumber() + " is already paid");
        }
        return toDto(send(invoice, PaymentReminder.Trigger.MANUAL, LocalDate.now(IST)), invoice);
    }

    @Transactional(readOnly = true)
    public List<PaymentReminderDto> forInvoice(Long invoiceId) {
        String tenantId = TenantContext.getTenantId();
        TradeDocument invoice = documentRepository.findByTenantIdAndId(tenantId, invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        return reminderRepository.findByTenantIdAndDocumentIdOrderBySentAtDesc(tenantId, invoiceId).stream()
                .map(r -> toDto(r, invoice))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentReminderDto> recent() {
        String tenantId = TenantContext.getTenantId();
        List<PaymentReminder> reminders = reminderRepository.findTop100ByTenantIdOrderBySentAtDesc(tenantId);
        Map<Long, TradeDocument> invoices = documentRepository.findByTenantIdAndIdIn(tenantId,
                        reminders.stream().map(PaymentReminder::getDocumentId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(TradeDocument::getId, Function.identity()));
        return reminders.stream().map(r -> toDto(r, invoices.get(r.getDocumentId()))).collect(Collectors.toList());
    }

    /**
     * Sends today's automatic reminders for the current tenant: a heads-up N days before the due
     * date, one on the due date, then every N days while overdue (up to a cap). Each trigger fires at
     * most once a day per invoice. Returns how many reminders were attempted.
     */
    @Transactional
    public int runForCurrentTenant(LocalDate today) {
        ReminderSettingsDto settings = getSettings();
        if (!settings.isEnabled()) {
            return 0;
        }
        String tenantId = TenantContext.getTenantId();
        Instant startOfDay = today.atStartOfDay(IST).toInstant();
        int attempted = 0;
        for (TradeDocument invoice : documentRepository.findByTenantIdAndDocTypeInAndStatusIn(tenantId,
                Set.of(DocumentType.SALES_INVOICE), Set.of(DocumentStatus.UNPAID, DocumentStatus.PARTIALLY_PAID))) {
            PaymentReminder.Trigger trigger = triggerFor(invoice, settings, today);
            if (trigger == null
                    || reminderRepository.existsByTenantIdAndDocumentIdAndTriggerTypeAndSentAtAfter(tenantId, invoice.getId(), trigger, startOfDay)) {
                continue;
            }
            send(invoice, trigger, today);
            attempted++;
        }
        return attempted;
    }

    PaymentReminder.Trigger triggerFor(TradeDocument invoice, ReminderSettingsDto settings, LocalDate today) {
        LocalDate due = invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getDocDate();
        long daysToDue = ChronoUnit.DAYS.between(today, due);
        if (settings.getDaysBeforeDue() > 0 && daysToDue == settings.getDaysBeforeDue()) {
            return PaymentReminder.Trigger.BEFORE_DUE;
        }
        if (settings.isOnDueDate() && daysToDue == 0) {
            return PaymentReminder.Trigger.ON_DUE;
        }
        long overdue = -daysToDue;
        if (overdue > 0 && settings.getOverdueEveryDays() > 0 && overdue % settings.getOverdueEveryDays() == 0) {
            long sent = reminderRepository.countByTenantIdAndDocumentIdAndTriggerTypeAndStatus(TenantContext.getTenantId(),
                    invoice.getId(), PaymentReminder.Trigger.OVERDUE, PaymentReminder.Status.SENT);
            if (sent < settings.getMaxOverdueReminders()) {
                return PaymentReminder.Trigger.OVERDUE;
            }
        }
        return null;
    }

    private PaymentReminder send(TradeDocument invoice, PaymentReminder.Trigger trigger, LocalDate today) {
        String tenantId = TenantContext.getTenantId();
        Party customer = partyRepository.findByTenantIdAndId(tenantId, invoice.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Tenant company = tenantRepository.findById(tenantId).orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        String message = message(company, customer, invoice, today);
        String phone = e164(customer.getPhone());

        PaymentReminder reminder = PaymentReminder.builder()
                .documentId(invoice.getId())
                .partyId(customer.getId())
                .channel(whatsApp.channel())
                .recipient(phone)
                .triggerType(trigger)
                .message(message)
                .sentAt(Instant.now())
                .createdBy(SecurityUtils.getCurrentUsername().orElse("scheduler"))
                .build();
        reminder.setTenantId(tenantId);
        if (phone == null) {
            reminder.setStatus(PaymentReminder.Status.SKIPPED);
            reminder.setError("No valid mobile number for " + customer.getName());
        } else {
            try {
                whatsApp.send(phone, message);
                reminder.setStatus(PaymentReminder.Status.SENT);
            } catch (RuntimeException e) {
                log.warn("WhatsApp reminder for {} failed: {}", invoice.getDocNumber(), e.getMessage());
                reminder.setStatus(PaymentReminder.Status.FAILED);
                reminder.setError(StringUtils.truncate(e.getMessage() == null ? "Send failed" : e.getMessage(), 250));
            }
        }
        return reminderRepository.save(reminder);
    }

    String message(Tenant company, Party customer, TradeDocument invoice, LocalDate today) {
        LocalDate due = invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getDocDate();
        long daysToDue = ChronoUnit.DAYS.between(today, due);
        String when = daysToDue > 0 ? "is due on " + due.format(DATE)
                : daysToDue == 0 ? "is due today"
                : "was due on " + due.format(DATE) + " (" + (-daysToDue) + " day" + (daysToDue == -1 ? "" : "s") + " overdue)";
        String from = StringUtils.hasText(company.getLegalName()) ? company.getLegalName() : company.getName();
        return "Hello " + customer.getName() + ", this is a reminder from " + from + " that invoice " + invoice.getDocNumber()
                + " for " + rupees(invoice.getBalance()) + " " + when + ". Please ignore this message if you've already paid. Thank you!";
    }

    /** Indian mobile numbers to E.164 (+91XXXXXXXXXX); null when it isn't one. */
    static String e164(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return digits.length() == 10 && digits.charAt(0) >= '6' ? "+91" + digits : null;
    }

    private static String rupees(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(amount);
    }

    private PaymentReminderDto toDto(PaymentReminder r, TradeDocument invoice) {
        return PaymentReminderDto.builder()
                .id(r.getId())
                .documentId(r.getDocumentId())
                .documentNumber(invoice == null ? null : invoice.getDocNumber())
                .partyName(invoice == null ? null : invoice.getPartyName())
                .channel(r.getChannel())
                .recipient(r.getRecipient())
                .triggerType(r.getTriggerType().name())
                .status(r.getStatus().name())
                .message(r.getMessage())
                .error(r.getError())
                .sentAt(r.getSentAt())
                .build();
    }

    private ReminderSettingsDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, ReminderSettingsDto.class);
        } catch (JsonProcessingException e) {
            return ReminderSettingsDto.builder().build();
        }
    }

    private String toJson(ReminderSettingsDto settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
