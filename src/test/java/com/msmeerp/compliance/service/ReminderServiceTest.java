package com.msmeerp.compliance.service;

import com.msmeerp.compliance.dto.ReminderSettingsDto;
import com.msmeerp.compliance.entity.PaymentReminder;
import com.msmeerp.compliance.repository.PaymentReminderRepository;
import com.msmeerp.tenant.context.TenantContext;
import com.msmeerp.tenant.entity.Tenant;
import com.msmeerp.trade.entity.Party;
import com.msmeerp.trade.entity.TradeDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private PaymentReminderRepository reminderRepository;

    @InjectMocks
    private ReminderService reminderService;

    private final ReminderSettingsDto settings = ReminderSettingsDto.builder()
            .enabled(true).daysBeforeDue(3).onDueDate(true).overdueEveryDays(7).maxOverdueReminders(2).build();
    private final LocalDate today = LocalDate.of(2026, 10, 12);

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId("tenant-1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static TradeDocument invoiceDue(LocalDate due) {
        TradeDocument invoice = TradeDocument.builder().docNumber("INV-0004").docDate(due.minusDays(15)).dueDate(due)
                .totalAmount(new BigDecimal("1027.00")).build();
        invoice.setId(27L);
        return invoice;
    }

    @Test
    void triggerFor_picksBeforeDueOnDueAndEveryNthOverdueDay() {
        assertThat(reminderService.triggerFor(invoiceDue(today.plusDays(3)), settings, today)).isEqualTo(PaymentReminder.Trigger.BEFORE_DUE);
        assertThat(reminderService.triggerFor(invoiceDue(today.plusDays(2)), settings, today)).isNull();
        assertThat(reminderService.triggerFor(invoiceDue(today), settings, today)).isEqualTo(PaymentReminder.Trigger.ON_DUE);
        assertThat(reminderService.triggerFor(invoiceDue(today.minusDays(5)), settings, today)).isNull();
    }

    @Test
    void triggerFor_stopsOverdueRemindersAtTheCap() {
        when(reminderRepository.countByTenantIdAndDocumentIdAndTriggerTypeAndStatus(anyString(), anyLong(), any(), any()))
                .thenReturn(1L, 2L);
        assertThat(reminderService.triggerFor(invoiceDue(today.minusDays(7)), settings, today)).isEqualTo(PaymentReminder.Trigger.OVERDUE);
        assertThat(reminderService.triggerFor(invoiceDue(today.minusDays(14)), settings, today)).isNull();
    }

    @Test
    void e164_normalisesIndianMobilesAndRejectsTheRest() {
        assertThat(ReminderService.e164("+91 98220 44551")).isEqualTo("+919822044551");
        assertThat(ReminderService.e164("09822044551")).isEqualTo("+919822044551");
        assertThat(ReminderService.e164("9822044551")).isEqualTo("+919822044551");
        assertThat(ReminderService.e164("020-2711 2233")).isNull();
        assertThat(ReminderService.e164(null)).isNull();
    }

    @Test
    void message_saysHowOverdueTheInvoiceIs() {
        Tenant company = new Tenant();
        company.setLegalName("Sakshi Engineering Works");
        Party customer = Party.builder().name("Shree Agro Traders").build();

        String message = reminderService.message(company, customer, invoiceDue(today.minusDays(3)), today);

        assertThat(message).contains("Shree Agro Traders", "Sakshi Engineering Works", "INV-0004", "₹1,027.00", "3 days overdue");
    }
}
