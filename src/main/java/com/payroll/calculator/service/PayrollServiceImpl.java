package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 시간 단위 분해 방식 (Map 기반)
 * - 각 시간을 순회하며 해당 날짜의 Map에 누적
 */
@Slf4j
@Service
public class PayrollServiceImpl implements PayrollService {

    private static final double OVERTIME_RATE = 0.5;
    private static final double NIGHT_RATE = 0.5;
    private static final double HOLIDAY_RATE = 0.5;
    private static final double HOLIDAY_OVERTIME_RATE = 1.0;
    private static final int DAILY_LIMIT = 8;
    private static final int WEEKLY_LIMIT = 40;
    private static final int WEEKLY_THRESHOLD = 15;

    @Override
    public PayrollResponse calculate(PayrollRequest request) {
        int wage = request.getWage();
        int year = request.getYear();
        int month = request.getMonth();

        Map<Integer, Integer> dailyHours = new HashMap<>();
        Map<Integer, Integer> nightHours = new HashMap<>();
        Map<Integer, Integer> holidayHoursUnder8 = new HashMap<>();
        Map<Integer, Integer> holidayHoursOver8 = new HashMap<>();
        Map<Integer, Integer> weeklyHours = new HashMap<>();

        for (WorkRecordRequest record : request.getRecords()) {
            int startIdx = (record.getStartDay() - 1) * 24 + record.getStartHour();
            int endIdx = (record.getEndDay() - 1) * 24 + record.getEndHour();

            for (int i = startIdx; i < endIdx; i++) {
                int day = (i / 24) + 1;
                int hour = i % 24;
                int week = (day - 1) / 7 + 1;
                LocalDate date = LocalDate.of(year, month, day);
                boolean isSunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

                dailyHours.merge(day, 1, Integer::sum);
                weeklyHours.merge(week, 1, Integer::sum);

                if (hour >= 22 || hour < 6) {
                    nightHours.merge(day, 1, Integer::sum);
                }

                if (isSunday) {
                    int currentHoliday = holidayHoursUnder8.getOrDefault(day, 0)
                            + holidayHoursOver8.getOrDefault(day, 0);
                    if (currentHoliday < 8) {
                        holidayHoursUnder8.merge(day, 1, Integer::sum);
                    } else {
                        holidayHoursOver8.merge(day, 1, Integer::sum);
                    }
                }
            }
        }

        int totalHours = dailyHours.values().stream().mapToInt(Integer::intValue).sum();
        int overtimeHoursTotal = dailyHours.values().stream()
                .mapToInt(h -> Math.max(0, h - DAILY_LIMIT)).sum();
        int nightHoursTotal = nightHours.values().stream().mapToInt(Integer::intValue).sum();
        int holidayUnder8Total = holidayHoursUnder8.values().stream().mapToInt(Integer::intValue).sum();
        int holidayOver8Total = holidayHoursOver8.values().stream().mapToInt(Integer::intValue).sum();

        long basePay = (long) totalHours * wage;
        long overtimePay = (long) (overtimeHoursTotal * wage * OVERTIME_RATE);
        long nightPay = (long) (nightHoursTotal * wage * NIGHT_RATE);
        long holidayPay = (long) (holidayUnder8Total * wage * HOLIDAY_RATE)
                + (long) (holidayOver8Total * wage * HOLIDAY_OVERTIME_RATE);
        long weeklyHolidayPay = calculateWeeklyHolidayPay(weeklyHours, wage);
        long totalPay = basePay + overtimePay + nightPay + holidayPay + weeklyHolidayPay;

        log.info("Map방식 계산완료 - total: {}", totalPay);

        return PayrollResponse.builder()
                .totalWorkHours(totalHours)
                .overtimeHours(overtimeHoursTotal)
                .nightHours(nightHoursTotal)
                .holidayHours(holidayUnder8Total + holidayOver8Total)
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .holidayPay(holidayPay)
                .weeklyHolidayPay(weeklyHolidayPay)
                .totalPay(totalPay)
                .build();
    }

    private long calculateWeeklyHolidayPay(Map<Integer, Integer> weeklyHours, int wage) {
        return weeklyHours.entrySet().stream()
                .filter(e -> e.getValue() >= WEEKLY_THRESHOLD)
                .mapToLong(e -> {
                    int hours = Math.min(e.getValue(), WEEKLY_LIMIT);
                    return (long) ((hours / (double) WEEKLY_LIMIT) * 8 * wage);
                })
                .sum();
    }
}