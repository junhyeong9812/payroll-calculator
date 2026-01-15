package com.payroll.calculator.controller;

import com.payroll.calculator.dto.ApiResponse;
import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<PayrollResponse>> calculate(@Valid @RequestBody PayrollRequest request) {
        log.info("급여 계산 요청 - records: {}, wage: {}, year: {}, month: {}",
                request.getRecords().size(), request.getWage(), request.getYear(), request.getMonth());

        PayrollResponse response = payrollService.calculate(request);

        log.info("급여 계산 완료 - totalPay: {}", response.getTotalPay());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("OK"));
    }
}