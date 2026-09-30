package com.campusbite.billing;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MonthlyStatementScheduler {

    private final BillingService billingService;
    private final java.time.ZoneId campusZone;

    public MonthlyStatementScheduler(BillingService billingService,
                                     @org.springframework.beans.factory.annotation.Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.billingService = billingService;
        this.campusZone = java.time.ZoneId.of(timeZone);
    }

    /** 1 AM on the 1st of every month -- bills whichever month just ended. */
    @Scheduled(cron = "0 0 1 1 * ?", zone = "${campusbite.time-zone:Asia/Kolkata}")
    public void generatePreviousMonthStatements() {
        LocalDate previousMonth = LocalDate.now(campusZone).minusMonths(1);
        billingService.generateStatementsFor(previousMonth.getYear(), previousMonth.getMonthValue());
    }
}