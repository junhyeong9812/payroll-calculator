package com.payroll.calculator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import com.payroll.calculator.exception.GlobalExceptionHandler;
import com.payroll.calculator.exception.PayrollException;
import com.payroll.calculator.service.PayrollService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
@Import(GlobalExceptionHandler.class)
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayrollService payrollService;

    @Test
    @DisplayName("헬스체크 성공")
    void health_success() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("OK"));
    }

    @Test
    @DisplayName("급여 계산 성공")
    void calculate_success() throws Exception {
        // given
        PayrollRequest request = createRequest(10000, 2025, 1,
                createWorkRecord(1, 9, 1, 18));

        PayrollResponse response = PayrollResponse.builder()
                .totalWorkHours(9.0)
                .overtimeHours(1.0)
                .nightHours(0.0)
                .holidayHours(0.0)
                .basePay(90000)
                .overtimePay(5000)
                .nightPay(0)
                .holidayPay(0)
                .weeklyHolidayPay(0)
                .totalPay(95000)
                .build();

        given(payrollService.calculate(any(PayrollRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/payroll/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalWorkHours").value(9.0))
                .andExpect(jsonPath("$.data.totalPay").value(95000));
    }

    @Test
    @DisplayName("급여 계산 실패 - 빈 근무기록")
    void calculate_fail_emptyRecords() throws Exception {
        // given
        PayrollRequest request = new PayrollRequest();
        request.setRecords(List.of());
        request.setWage(10000);
        request.setYear(2025);
        request.setMonth(1);

        // when & then
        mockMvc.perform(post("/api/v1/payroll/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("급여 계산 실패 - 시급 누락")
    void calculate_fail_noWage() throws Exception {
        // given
        PayrollRequest request = new PayrollRequest();
        request.setRecords(List.of(createWorkRecord(1, 9, 1, 18)));
        request.setYear(2025);
        request.setMonth(1);

        // when & then
        mockMvc.perform(post("/api/v1/payroll/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("급여 계산 실패 - 서비스 예외 발생")
    void calculate_fail_serviceException() throws Exception {
        // given
        PayrollRequest request = createRequest(10000, 2025, 1,
                createWorkRecord(1, 9, 1, 18));

        given(payrollService.calculate(any(PayrollRequest.class)))
                .willThrow(new PayrollException(400, "잘못된 근무 기록입니다"));

        // when & then
        mockMvc.perform(post("/api/v1/payroll/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 근무 기록입니다"));
    }

    private PayrollRequest createRequest(int wage, int year, int month, WorkRecordRequest... records) {
        PayrollRequest request = new PayrollRequest();
        request.setRecords(List.of(records));
        request.setWage(wage);
        request.setYear(year);
        request.setMonth(month);
        return request;
    }

    private WorkRecordRequest createWorkRecord(int startDay, int startHour, int endDay, int endHour) {
        WorkRecordRequest record = new WorkRecordRequest();
        record.setStartDay(startDay);
        record.setStartHour(startHour);
        record.setEndDay(endDay);
        record.setEndHour(endHour);
        return record;
    }
}