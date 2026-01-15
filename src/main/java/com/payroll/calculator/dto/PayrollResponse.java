package com.payroll.calculator.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayrollResponse {

    // 근무 시간
    private double totalWorkHours;
    private double overtimeHours;
    private double nightHours;
    private double holidayHours;

    // 수당
    private long basePay;
    private long overtimePay;
    private long nightPay;
    private long holidayPay;
    private long weeklyHolidayPay;

    // 총액
    private long totalPay;
}