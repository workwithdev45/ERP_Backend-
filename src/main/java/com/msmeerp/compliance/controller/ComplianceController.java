package com.msmeerp.compliance.controller;

import com.msmeerp.common.response.ApiResponse;
import com.msmeerp.compliance.dto.CancelRequest;
import com.msmeerp.compliance.dto.ComplianceDto;
import com.msmeerp.compliance.dto.EwayBillRequest;
import com.msmeerp.compliance.dto.PaymentReminderDto;
import com.msmeerp.compliance.dto.ReminderSettingsDto;
import com.msmeerp.compliance.service.EInvoiceService;
import com.msmeerp.compliance.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** W13: e-invoice, e-way bill and payment reminders for sales invoices. */
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class ComplianceController {

    private static final String CAN_VIEW = "hasRole('ADMIN') or hasAnyAuthority('SALES_VIEW', 'SALES_READ', 'SALES_WRITE')";
    private static final String CAN_CREATE = "hasRole('ADMIN') or hasAnyAuthority('SALES_CREATE', 'SALES_WRITE')";
    private static final String CAN_EDIT = "hasRole('ADMIN') or hasAnyAuthority('SALES_EDIT', 'SALES_WRITE')";

    private final EInvoiceService einvoiceService;
    private final ReminderService reminderService;

    @GetMapping("/invoices/{id}/compliance")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<ComplianceDto>> compliance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(einvoiceService.status(id)));
    }

    @PostMapping("/invoices/{id}/einvoice")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<ComplianceDto>> generateIrn(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(einvoiceService.generateIrn(id), "IRN generated"));
    }

    @PostMapping("/invoices/{id}/einvoice/cancel")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<ComplianceDto>> cancelIrn(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(einvoiceService.cancelIrn(id, request), "IRN cancelled"));
    }

    @PostMapping("/invoices/{id}/eway-bills")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<ComplianceDto>> generateEwayBill(@PathVariable Long id, @Valid @RequestBody EwayBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success(einvoiceService.generateEwayBill(id, request), "E-way bill generated"));
    }

    @PostMapping("/eway-bills/{id}/cancel")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<ComplianceDto>> cancelEwayBill(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(einvoiceService.cancelEwayBill(id, request), "E-way bill cancelled"));
    }

    @GetMapping("/invoices/{id}/reminders")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<PaymentReminderDto>>> invoiceReminders(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.forInvoice(id)));
    }

    @PostMapping("/invoices/{id}/reminders")
    @PreAuthorize(CAN_CREATE)
    public ResponseEntity<ApiResponse<PaymentReminderDto>> sendReminder(@PathVariable Long id) {
        PaymentReminderDto reminder = reminderService.sendNow(id);
        return ResponseEntity.ok(ApiResponse.success(reminder, "SENT".equals(reminder.getStatus()) ? "Reminder sent" : "Reminder not sent"));
    }

    @GetMapping("/reminders")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<List<PaymentReminderDto>>> recentReminders() {
        return ResponseEntity.ok(ApiResponse.success(reminderService.recent()));
    }

    /** Runs today's automatic reminders now instead of waiting for the 09:00 job. */
    @PostMapping("/reminders/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> runReminders() {
        int attempted = reminderService.runForCurrentTenant(LocalDate.now(ZoneId.of("Asia/Kolkata")));
        return ResponseEntity.ok(ApiResponse.success(Map.of("attempted", attempted), attempted + " reminder(s) processed"));
    }

    @GetMapping("/reminder-settings")
    @PreAuthorize(CAN_VIEW)
    public ResponseEntity<ApiResponse<ReminderSettingsDto>> reminderSettings() {
        return ResponseEntity.ok(ApiResponse.success(reminderService.getSettings()));
    }

    @PutMapping("/reminder-settings")
    @PreAuthorize(CAN_EDIT)
    public ResponseEntity<ApiResponse<ReminderSettingsDto>> updateReminderSettings(@Valid @RequestBody ReminderSettingsDto settings) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.updateSettings(settings), "Reminder settings saved"));
    }
}
