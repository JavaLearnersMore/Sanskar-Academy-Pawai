package com.sanskaracademy.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sanskaracademy.service.AuditService;
import com.sanskaracademy.service.EnquiryService;

/** Daily housekeeping: enquiry follow-up reminder to the admins, and old audit lines removed. */
@Component
public class OfficeJobs {

    private final EnquiryService enquiries;
    private final AuditService audit;

    public OfficeJobs(EnquiryService enquiries, AuditService audit) {
        this.enquiries = enquiries;
        this.audit = audit;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void followUps() {
        try {
            enquiries.notifyAdminsOfDue();
        } catch (RuntimeException ignored) {
            // try again tomorrow
        }
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Kolkata")
    public void purgeAudit() {
        try {
            audit.purge();
        } catch (RuntimeException ignored) {
            // try again tomorrow
        }
    }
}
