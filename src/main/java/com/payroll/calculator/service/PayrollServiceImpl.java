package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PayrollServiceImpl implements PayrollService {

    @Override
    public PayrollResponse calculate(PayrollRequest request) {
        // TODO: 구현 예정
        log.info("calculate 호출됨");
        return PayrollResponse.builder()
                .totalWorkHours(0)
                .overtimeHours(0)
                .nightHours(0)
                .holidayHours(0)
                .basePay(0)
                .overtimePay(0)
                .nightPay(0)
                .holidayPay(0)
                .weeklyHolidayPay(0)
                .totalPay(0)
                .build();
    }
}