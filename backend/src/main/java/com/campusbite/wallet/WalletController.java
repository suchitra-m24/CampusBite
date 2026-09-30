package com.campusbite.wallet;

import com.campusbite.common.security.SecurityUser;
import com.campusbite.common.security.RateLimitService;
import com.campusbite.wallet.dto.TopUpRequest;
import com.campusbite.wallet.dto.WalletDto;
import com.campusbite.wallet.dto.WalletTransactionDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/wallet")
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionRepository transactionRepository;
    private final RateLimitService rateLimitService;

    public WalletController(WalletService walletService, WalletTransactionRepository transactionRepository, RateLimitService rateLimitService) {
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public WalletDto getWallet(@AuthenticationPrincipal SecurityUser principal) {
        return new WalletDto(walletService.getOrCreate(principal.getUser()).getBalance());
    }

    @PostMapping("/top-up")
    public WalletDto topUp(@AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody TopUpRequest request) {
        rateLimitService.check("topup:" + principal.getId(), 20);
        return new WalletDto(walletService.topUp(principal.getUser(), request.amount()).getBalance());
    }

    @GetMapping("/transactions")
    public List<WalletTransactionDto> transactions(@AuthenticationPrincipal SecurityUser principal) {
        Wallet wallet = walletService.getOrCreate(principal.getUser());
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet).stream()
                .map(WalletTransactionDto::from).toList();
    }
}
