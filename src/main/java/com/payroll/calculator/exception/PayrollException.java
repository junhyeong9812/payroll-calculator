package com.payroll.calculator.exception;

import lombok.Getter;

@Getter
public class PayrollException extends RuntimeException {

    private final int status;

    public PayrollException(int status, String message) {
        super(message);
        this.status = status;
    }
}