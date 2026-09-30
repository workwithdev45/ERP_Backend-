package com.msmeerp.compliance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** When automatic WhatsApp payment reminders go out for unpaid invoices. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderSettingsDto {
    @Builder.Default
    private boolean enabled = false;

    /** Days before the due date to send a heads-up; 0 turns it off. */
    @Min(0)
    @Max(30)
    @Builder.Default
    private int daysBeforeDue = 3;

    @Builder.Default
    private boolean onDueDate = true;

    /** Repeat every N days while overdue; 0 turns it off. */
    @Min(0)
    @Max(60)
    @Builder.Default
    private int overdueEveryDays = 7;

    /** Stop after this many overdue reminders per invoice. */
    @Min(0)
    @Max(20)
    @Builder.Default
    private int maxOverdueReminders = 3;
}
