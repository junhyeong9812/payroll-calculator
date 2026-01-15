package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;

public interface PayrollService {

    PayrollResponse calculate(PayrollRequest request);
}