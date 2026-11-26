package com.kdt.nbe13152team07.payment.controller;

import com.kdt.nbe13152team07.global.security.LoginUser;
import com.kdt.nbe13152team07.payment.dto.PaymentLinkRequest;
import com.kdt.nbe13152team07.payment.dto.PaymentLinkResponse;
import com.kdt.nbe13152team07.payment.service.PaymentLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-links")
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    public PaymentLinkController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    // L1 통장 연결
    @PostMapping
    public ResponseEntity<PaymentLinkResponse> link(@AuthenticationPrincipal LoginUser loginUser,
                                                    @Valid @RequestBody PaymentLinkRequest request) {
        PaymentLinkResponse response = paymentLinkService.link(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // L2 내 통장 연결 조회
    @GetMapping("/me")
    public ResponseEntity<PaymentLinkResponse> get(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(paymentLinkService.get(loginUser.userId()));
    }

    // L3 통장 연결 해제
    @DeleteMapping("/me")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal LoginUser loginUser) {
        paymentLinkService.unlink(loginUser.userId());
        return ResponseEntity.noContent().build();
    }
}