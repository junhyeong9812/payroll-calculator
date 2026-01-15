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
        payrollService = new PayrollServiceImpl();
    }

    @Nested
    @DisplayName("기본급 계산")
    class BasePayTest {

        @Test
        @DisplayName("9시~ 18시 근무 (9시간) - 기본급 90,000원")
        void calculate_9hours() {

        }

        @Test
        @DisplayName("하루 두번 출근 (4시간 + 5시간 = 9시간)")
        void calculate_twoShifts() {

        }
    }

    @Nested
    @DisplayName("연장근로수당 계산 (50%)")
    class OverTimePayTest {

        @Test
        @DisplayName("9시간 근무 - 1시간 연장근로")
        void calculate_1hour_overtime() {

        }

        @Test
        @DisplayName("하루 두 번 출근 합산 9시간 - 1시간 연장 근로")
        void calculate_twoShifts_overtime() {

        }

        @Test
        @DisplayName("8시간 근무 - 연장근로 없음")
        void calculate_no_overtime() {

        }
    }

    @Nested
    @DisplayName("야간근로수당 계산 (22시~06시, 50%)")
    class NightPayTest {

        @Test
        @DisplayName("22시~02시 근무 (4시간 야간)")
        void calculate_nightShift() {

        }

        @Test
        @DisplayName("20시~24시 근무 (2시간 야간: 22시~24시)")
        void calculate_partialNight() {

        }

    }

    @Nested
    @DisplayName("휴일근로수당 계산 (일요일)")
    class HolidayPayTest {

        @Test
        @DisplayName("일요일 8시간 근무 - 휴일수당 50%")
        void calculate_holiday_8hours() {

        }

        @Test
        @DisplayName("일요일 10시간 근무 - 8시간 50%, 2시간 100%")
        void calculate_holiday_10hours() {

        }
    }

    @Nested
    @DisplayName("주휴수당 계산 (주 15시간 이상)")
    class WeeklyHolidayPayTest {

        @Test
        @DisplayName("주 15시간 근무 - 주휴수당 발생")
        void calculate_weeklyHoliday_15hours() {
        }

        @Test
        @DisplayName("주 14시간 근무 - 주휴수당 없음")
        void calculate_noWeeklyHoliday() {
        }

        @Test
        @DisplayName("주 40시간 이상 근무 - 최대 8시간분")
        void calculate_weeklyHoliday_max() {
        }
    }

    @Nested
    @DisplayName("복합 케이스")
    class ComplexCaseTest {

        @Test
        @DisplayName("3일 연속 근무 (금~일) - 날짜별 분리 계산")
        void calculate_3days_continuous() {

        }
    }
}