package com.smartpark.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.service.BookingService;
import com.smartpark.service.HmacService;
import com.smartpark.service.MonthlyPassService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final BookingService service;
    private final MonthlyPassService monthlyPassService;
    private final HmacService hmacService;
    private final ObjectMapper objectMapper;

    public WebhookController(BookingService service,
                             MonthlyPassService monthlyPassService,
                             HmacService hmacService,
                             ObjectMapper objectMapper) {
        this.service = service;
        this.monthlyPassService = monthlyPassService;
        this.hmacService = hmacService;
        this.objectMapper = objectMapper;
    }

    /**
     * SePay POST về đây khi có tiền vào tài khoản.
     * Điền URL này vào SePay: https://smartpark-2.onrender.com/webhook/sepay
     */
    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> handleSePay(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) throws JsonProcessingException {

        String signature = request.getHeader("X-SePay-Signature");
        String rawPayload = objectMapper.writeValueAsString(payload);

        if (!hmacService.verify(rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid signature"));
        }

        System.out.println("[WEBHOOK] " + payload);

        String content      = (String) payload.get("content");
        String transferType = (String) payload.get("transferType");
        Object amountRaw    = payload.get("transferAmount");
        long   amount       = amountRaw instanceof Number n ? n.longValue() : 0L;
        String bankRef      = String.valueOf(payload.getOrDefault("referenceCode", ""));

        if (!"in".equals(transferType))
            return ResponseEntity.ok(Map.of("success", true, "message", "ignored"));

        // Thử xử lý thanh toán vé thường (SP...) trước
        boolean ok = service.processPayment(content, amount, bankRef);

        // Nếu không khớp vé thường, thử thẻ tháng (MP...)
        if (!ok) {
            ok = monthlyPassService.processPayment(content, amount, bankRef);
        }

        return ResponseEntity.ok(Map.of("success", true, "confirmed", ok));
    }

    /**
     * Test thủ công – dùng khi demo không có chuyển khoản thật
     * Gọi bằng trình duyệt:
     * GET https://smartpark-2.onrender.com/webhook/test-pay?code=SPXXXXXX&amount=5000
     */
    @GetMapping("/test-pay")
    public ResponseEntity<Map<String, Object>> testPay(
            @RequestParam String code,
            @RequestParam long amount) {

        System.out.println("[TEST-PAY] code=" + code + " amount=" + amount);

        // Thử vé thường (SP...) trước, sau đó thẻ tháng (MP...)
        boolean ok = service.processPayment(code, amount, "TEST-REF");
        if (!ok) {
            ok = monthlyPassService.processPayment(code, amount, "TEST-REF");
        }

        return ResponseEntity.ok(Map.of("success", true, "confirmed", ok));
    }
}