package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollServiceTest {

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
//        payrollService = new PayrollServiceImpl();
//        payrollService = new PayrollServiceTimelineImpl();
        payrollService = new PayrollServiceLegacyImpl();
    }

    @Nested
    @DisplayName("기본급 계산")
    class BasePayTest {

        @Test
        @DisplayName("9시~18시 근무 (9시간) - 기본급 90,000원")
        void calculate_9hours() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 18));

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getTotalWorkHours()).isEqualTo(9.0);
            assertThat(response.getBasePay()).isEqualTo(90000);
        }

        @Test
        @DisplayName("하루 두 번 출근 (4시간 + 5시간 = 9시간)")
        void calculate_twoShifts() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(15, 9, 15, 13),   // 4시간
                    createWorkRecord(15, 18, 15, 23)); // 5시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getTotalWorkHours()).isEqualTo(9.0);
            assertThat(response.getBasePay()).isEqualTo(90000);
        }
    }

    @Nested
    @DisplayName("연장근로수당 계산 (50%)")
    class OvertimePayTest {

        @Test
        @DisplayName("9시간 근무 - 1시간 연장근로")
        void calculate_1hour_overtime() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 18)); // 9시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getOvertimeHours()).isEqualTo(1.0);
            assertThat(response.getOvertimePay()).isEqualTo(5000); // 1시간 * 10000 * 0.5
        }

        @Test
        @DisplayName("하루 두 번 출근 합산 9시간 - 1시간 연장근로")
        void calculate_twoShifts_overtime() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(15, 9, 15, 13),   // 4시간
                    createWorkRecord(15, 18, 15, 23)); // 5시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getOvertimeHours()).isEqualTo(1.0);
            assertThat(response.getOvertimePay()).isEqualTo(5000);
        }

        @Test
        @DisplayName("8시간 근무 - 연장근로 없음")
        void calculate_no_overtime() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 17)); // 8시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getOvertimeHours()).isEqualTo(0.0);
            assertThat(response.getOvertimePay()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("야간근로수당 계산 (22시~06시, 50%)")
    class NightPayTest {

        @Test
        @DisplayName("22시~02시 근무 (4시간 야간)")
        void calculate_nightShift() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 22, 2, 2)); // 4시간 전부 야간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getNightHours()).isEqualTo(4.0);
            assertThat(response.getNightPay()).isEqualTo(20000); // 4 * 10000 * 0.5
        }

        @Test
        @DisplayName("20시~24시 근무 (2시간 야간: 22시~24시)")
        void calculate_partialNight() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 20, 2, 0)); // 4시간 중 2시간 야간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getTotalWorkHours()).isEqualTo(4.0);
            assertThat(response.getNightHours()).isEqualTo(2.0);
            assertThat(response.getNightPay()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("휴일근로수당 계산 (일요일)")
    class HolidayPayTest {

        @Test
        @DisplayName("일요일 8시간 근무 - 휴일수당 50%")
        void calculate_holiday_8hours() {
            // given: 2025년 1월 5일 = 일요일
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(5, 9, 5, 17)); // 8시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getHolidayHours()).isEqualTo(8.0);
            assertThat(response.getHolidayPay()).isEqualTo(40000); // 8 * 10000 * 0.5
        }

        @Test
        @DisplayName("일요일 10시간 근무 - 8시간 50%, 2시간 100%")
        void calculate_holiday_10hours() {
            // given: 2025년 1월 5일 = 일요일
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(5, 9, 5, 19)); // 10시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getHolidayHours()).isEqualTo(10.0);
            // 8시간 * 0.5 + 2시간 * 1.0 = 40000 + 20000
            assertThat(response.getHolidayPay()).isEqualTo(60000);
        }
    }

    @Nested
    @DisplayName("주휴수당 계산 (주 15시간 이상)")
    class WeeklyHolidayPayTest {

        @Test
        @DisplayName("주 15시간 근무 - 주휴수당 발생")
        void calculate_weeklyHoliday_15hours() {
            // given: 1주차에 15시간
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 14),  // 5시간
                    createWorkRecord(2, 9, 2, 14),  // 5시간
                    createWorkRecord(3, 9, 3, 14)); // 5시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then: (15/40) * 8 * 10000 = 30000
            assertThat(response.getWeeklyHolidayPay()).isEqualTo(30000);
        }

        @Test
        @DisplayName("주 14시간 근무 - 주휴수당 없음")
        void calculate_noWeeklyHoliday() {
            // given
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 16)); // 7시간 * 2일 = 14시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            assertThat(response.getWeeklyHolidayPay()).isEqualTo(0);
        }

        @Test
        @DisplayName("주 40시간 이상 근무 - 최대 8시간분")
        void calculate_weeklyHoliday_max() {
            // given: 주 45시간 근무해도 최대 8시간분
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(1, 9, 1, 18),  // 9시간
                    createWorkRecord(2, 9, 2, 18),  // 9시간
                    createWorkRecord(3, 9, 3, 18),  // 9시간
                    createWorkRecord(4, 9, 4, 18),  // 9시간
                    createWorkRecord(5, 9, 5, 18)); // 9시간 = 45시간

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then: 최대 8시간 * 10000 = 80000
            assertThat(response.getWeeklyHolidayPay()).isEqualTo(80000);
        }
    }

    @Nested
    @DisplayName("복합 케이스")
    class ComplexCaseTest {

        @Test
        @DisplayName("3일 연속 근무 (금~일) - 날짜별 분리 계산")
        void calculate_3days_continuous() {
            // given: 금(10일) 22시 ~ 월(13일) 06시
            // 2025년 1월: 10일=금, 11일=토, 12일=일, 13일=월
            PayrollRequest request = createRequest(10000, 2025, 1,
                    createWorkRecord(10, 22, 13, 6));

            // when
            PayrollResponse response = payrollService.calculate(request);

            // then
            // 총 근무: 56시간
            // - 10일(금): 22~24 = 2시간 (야간 2시간)
            // - 11일(토): 0~24 = 24시간 (야간 6시간)
            // - 12일(일): 0~24 = 24시간 (휴일 24시간, 야간 6시간)
            // - 13일(월): 0~6 = 6시간 (야간 6시간)
            assertThat(response.getTotalWorkHours()).isEqualTo(56.0);
            assertThat(response.getNightHours()).isEqualTo(24.0); // 2+8+8+8
            assertThat(response.getHolidayHours()).isEqualTo(24.0); // 일요일
        }
    }

    // Helper methods
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