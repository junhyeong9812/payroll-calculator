package com.payroll.calculator.service;


import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 타임라인 배열 방식
 * - boolean[32*24] 배열로 전체 월을 표현
 * - 단일 스캔으로 모든 수당 계산
 */
@Slf4j
public class PayrollServiceTimelineImpl implements PayrollService {

    private static final int MAX_HOURS = 32 * 24;
    private static final int HOURS_PER_DAY = 24;
    private static final int HOURS_PER_WEEK = 168;
    private static final int DAILY_LIMIT = 8;
    private static final int WEEKLY_LIMIT = 40;
    private static final int WEEKLY_THRESHOLD = 15;

    @Override
    public PayrollResponse calculate(PayrollRequest request) {
        int wage = request.getWage();
        int year = request.getYear();
        int month = request.getMonth();

        // 1. 타임라인 정규화
        boolean[] timeline = new boolean[MAX_HOURS];
        for (WorkRecordRequest record : request.getRecords()) {
            int start = (record.getStartDay() - 1) * HOURS_PER_DAY + record.getStartHour();
            int end = (record.getEndDay() - 1) * HOURS_PER_DAY + record.getEndHour();
            for (int i = start; i < end; i++) {
                timeline[i] = true;
            }
        }

        // 2. 단일 스캔으로 수당 계산
        long basePay = 0, overtimePay = 0, nightPay = 0, holidayPay = 0;
        int totalHours = 0, overtimeHours = 0, nightHours = 0, holidayHours = 0;
        int[] dailyHours = new int[32];
        int[] dailyHolidayHours = new int[32];

        for (int i = 0; i < timeline.length; i++) {
            if (!timeline[i]) continue;

            int day = (i / HOURS_PER_DAY) + 1;
            int hour = i % HOURS_PER_DAY;

            if (day > 31) break;

            LocalDate date = LocalDate.of(year, month, day);
            boolean isSunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

            // 기본급
            basePay += wage;
            totalHours++;
            dailyHours[day]++;

            // 야간 (22:00 ~ 06:00)
            if (hour >= 22 || hour < 6) {
                nightPay += (long) (wage * 0.5);
                nightHours++;
            }

            // 휴일 vs 연장
            if (isSunday) {
                dailyHolidayHours[day]++;
                holidayHours++;
                if (dailyHolidayHours[day] <= DAILY_LIMIT) {
                    holidayPay += (long) (wage * 0.5);
                } else {
                    holidayPay += wage;
                }
            } else {
                if (dailyHours[day] > DAILY_LIMIT) {
                    overtimePay += (long) (wage * 0.5);
                    overtimeHours++;
                }
            }
        }

        // 3.주휴수당
        long weeklyHolidayPay = calculateWeeklyHolidayPay(timeline, wage);
        long totalPay = basePay + overtimePay + nightPay + holidayPay + weeklyHolidayPay;

        log.info("Timeline방식 계산완료 - total: {}", totalPay);

        return PayrollResponse.builder()
                .totalWorkHours(totalHours)
                .overtimeHours(overtimeHours)
                .nightHours(nightHours)
                .holidayHours(holidayHours)
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .holidayPay(holidayPay)
                .weeklyHolidayPay(weeklyHolidayPay)
                .totalPay(totalPay)
                .build();
    }

    private long calculateWeeklyHolidayPay(boolean[] timeline, int wage) {
        long totalWeeklyPay = 0;
        for (int week = 0; week < 5; week++) {
            int weekStart = week * HOURS_PER_WEEK;
            int weekEnd = Math.min(weekStart + HOURS_PER_WEEK, timeline.length);
            int weeklyHours = 0;

            for (int i = weekStart; i < weekEnd; i++) {
                if (timeline[i]) weeklyHours++;
            }

            if (weeklyHours >= WEEKLY_THRESHOLD) {
                double holidayHours = Math.min(weeklyHours, WEEKLY_LIMIT) / (double) WEEKLY_LIMIT * 8;
                totalWeeklyPay += (long) (holidayHours * wage);
            }
        }
        return totalWeeklyPay;
    }
}
