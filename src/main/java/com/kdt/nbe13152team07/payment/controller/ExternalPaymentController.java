package com.kdt.nbe13152team07.payment.controller;

import com.kdt.nbe13152team07.payment.dto.PaymentApprovedRequest;
import com.kdt.nbe13152team07.payment.dto.PaymentReceiptResponse;
import com.kdt.nbe13152team07.payment.service.PaymentReceiveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 모의 결제 서버(mock-pay)가 호출하는 API. 사용자 로그인이 아니라 X-API-KEY로 인증한다.
@RestController
@RequestMapping("/api/external/payments")
public class ExternalPaymentController {

    private final PaymentReceiveService paymentReceiveService;

    public ExternalPaymentController(PaymentReceiveService paymentReceiveService) {
        this.paymentReceiveService = paymentReceiveService;
    }

    // X1 승인 결제 수신: 무시하는 경우(IGNORED_*, DUPLICATE)도 200으로 응답
    @PostMapping
    public ResponseEntity<PaymentReceiptResponse> receive(@Valid @RequestBody PaymentApprovedRequest request) {
        return ResponseEntity.ok(paymentReceiveService.receive(request));
    }
}
