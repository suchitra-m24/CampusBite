package com.campusbite.billing;

import com.campusbite.billing.dto.CurrentBillingDto;
import com.campusbite.billing.dto.GenerateStatementsRequest;
import com.campusbite.billing.dto.MonthlyStatementDto;
import com.campusbite.common.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/api/student/billing/current")
    public CurrentBillingDto currentBilling(@AuthenticationPrincipal SecurityUser principal) {
        return billingService.getCurrentBillingFor(principal.getUser());
    }

    @GetMapping("/api/student/billing/statements")
    public List<MonthlyStatementDto> myStatements(@AuthenticationPrincipal SecurityUser principal) {
        return billingService.getStatementsFor(principal.getUser());
    }

    @GetMapping("/api/admin/billing/statements")
    public List<MonthlyStatementDto> unsettledStatements() {
        return billingService.getUnsettledStatements();
    }

    @PostMapping("/api/admin/billing/generate")
    public List<MonthlyStatementDto> generate(@Valid @RequestBody GenerateStatementsRequest request) {
        return billingService.generateStatementsFor(request.year(), request.month());
    }

    @PostMapping("/api/admin/billing/statements/{id}/settle")
    public MonthlyStatementDto settle(@PathVariable Long id) {
        return billingService.settle(id);
    }
}