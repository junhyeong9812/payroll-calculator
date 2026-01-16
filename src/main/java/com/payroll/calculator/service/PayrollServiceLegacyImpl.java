package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 행 단위 계산 방식
 */
@Slf4j
public class PayrollServiceLegacyImpl implements PayrollService {

    private static final int DAILY_LIMIT = 8;
    private static final int WEEKLY_LIMIT = 40;
    private static final int WEEKLY_THRESHOLD = 15;
    private static final int NIGHT_START = 22;
    private static final int NIGHT_END = 6;

    private int year;
    private int month;

    @Override
    public PayrollResponse calculate(PayrollRequest request) {
        this.year = request.getYear();
        this.month = request.getMonth();
        int wage = request.getWage();

        List<int[]> works = toList(request.getRecords());

        Map<Integer, Integer> dailyHoursMap = buildDailyHoursMap(works);

        int totalHours = calculateTotalHours(works);
        int overtimeHours = calculateOvertimeHours(dailyHoursMap);
        int nightHours = calculateNightHours(works);
        int holidayHoursUnder8 = calculateHolidayHoursUnder8(works);
        int holidayHoursOver8 = calculateHolidayHoursOver8(works);

        long basePay = (long) totalHours * wage;
        long overtimePay = (long) (overtimeHours * wage * 0.5);
        long nightPay = (long) (nightHours * wage * 0.5);
        long holidayPay = (long) (holidayHoursUnder8 * wage * 0.5)
                + (long) (holidayHoursOver8 * wage * 1.0);
        long weeklyHolidayPay = calculateWeeklyHolidayPay(works, wage);
        long totalPay = basePay + overtimePay + nightPay + holidayPay + weeklyHolidayPay;

        log.info("Legacy방식 계산완료 - total: {}", totalPay);

        return PayrollResponse.builder()
                .totalWorkHours(totalHours)
                .overtimeHours(overtimeHours)
                .nightHours(nightHours)
                .holidayHours(holidayHoursUnder8 + holidayHoursOver8)
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .holidayPay(holidayPay)
                .weeklyHolidayPay(weeklyHolidayPay)
                .totalPay(totalPay)
                .build();
    }

    private List<int[]> toList(List<WorkRecordRequest> records) {
        return records.stream()
                .map(r -> new int[]{r.getStartDay(), r.getStartHour(), r.getEndDay(), r.getEndHour()})
                .toList();
    }

    // ========== 날짜별 시간 맵 ==========
    private Map<Integer, Integer> buildDailyHoursMap(List<int[]> works) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int[] work : works) {
            int startDay = work[0], startHour = work[1], endDay = work[2], endHour = work[3];

            if (startDay == endDay) {
                map.merge(startDay, endHour - startHour, Integer::sum);
            } else {
                map.merge(startDay, 24 - startHour, Integer::sum);
                for (int d = startDay + 1; d < endDay; d++) {
                    map.merge(d, 24, Integer::sum);
                }
                if (endHour > 0) map.merge(endDay, endHour, Integer::sum);
            }
        }
        return map;
    }

    // ========== 총 근무시간 ==========
    private int calculateTotalHours(List<int[]> works) {
        int total = 0;
        for (int[] work : works) {
            int startDay = work[0], startHour = work[1], endDay = work[2], endHour = work[3];
            if (startDay == endDay) {
                total += endHour - startHour;
            } else {
                total += (24 - startHour) + ((endDay - startDay - 1) * 24) + endHour;
            }
        }
        return total;
    }

    // ========== 연장근로 ==========
    private int calculateOvertimeHours(Map<Integer, Integer> dailyHoursMap) {
        return dailyHoursMap.values().stream()
                .mapToInt(h -> Math.max(0, h - DAILY_LIMIT))
                .sum();
    }

    // ========== 야간근로 (22시~06시) ==========
    private int calculateNightHours(List<int[]> works) {
        int total = 0;
        for (int[] work : works) {
            int startDay = work[0], startHour = work[1], endDay = work[2], endHour = work[3];

            if (startDay == endDay) {
                total += nightHoursInRange(startHour, endHour);
            } else {
                total += nightHoursInRange(startHour, 24);
                for (int d = startDay + 1; d < endDay; d++) {
                    total += 8; // 0~6(6h) + 22~24(2h)
                }
                total += nightHoursInRange(0, endHour);
            }
        }
        return total;
    }

    private int nightHoursInRange(int start, int end) {
        int night = 0;
        if (start < NIGHT_END) night += Math.min(end, NIGHT_END) - start;
        if (end > NIGHT_START) night += end - Math.max(start, NIGHT_START);
        return Math.max(0, night);
    }

    // ========== 휴일근로 (일요일) ==========
    private Map<Integer, Integer> buildHolidayHoursMap(List<int[]> works) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int[] work : works) {
            int startDay = work[0], startHour = work[1], endDay = work[2], endHour = work[3];

            if (startDay == endDay) {
                if (isSunday(startDay)) map.merge(startDay, endHour - startHour, Integer::sum);
            } else {
                if (isSunday(startDay)) map.merge(startDay, 24 - startHour, Integer::sum);
                for (int d = startDay + 1; d < endDay; d++) {
                    if (isSunday(d)) map.merge(d, 24, Integer::sum);
                }
                if (isSunday(endDay) && endHour > 0) map.merge(endDay, endHour, Integer::sum);
            }
        }
        return map;
    }

    private int calculateHolidayHoursUnder8(List<int[]> works) {
        return buildHolidayHoursMap(works).values().stream()
                .mapToInt(h -> Math.min(h, DAILY_LIMIT))
                .sum();
    }

    private int calculateHolidayHoursOver8(List<int[]> works) {
        return buildHolidayHoursMap(works).values().stream()
                .mapToInt(h -> Math.max(0, h - DAILY_LIMIT))
                .sum();
    }

    private boolean isSunday(int day) {
        return LocalDate.of(year, month, day).getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    // ========== 주휴수당 (ISO 주차 기반, 해당 월 내 근무만 계산) ==========
    private long calculateWeeklyHolidayPay(List<int[]> works, int wage) {
        // 해당 월의 모든 날짜를 ISO 주차별로 그룹핑
        Map<Integer, Integer> weeklyHoursMap = new HashMap<>();

        for (int[] work : works) {
            int startDay = work[0], startHour = work[1], endDay = work[2], endHour = work[3];

            // 해당 월 내의 날짜만 처리 (월 경계 넘어가는 건 무시)
            int lastDayOfMonth = LocalDate.of(year, month, 1).lengthOfMonth();
            int actualEndDay = Math.min(endDay, lastDayOfMonth);

            if (startDay == endDay) {
                int weekNum = getWeekNumber(startDay);
                weeklyHoursMap.merge(weekNum, endHour - startHour, Integer::sum);
            } else {
                // 첫날
                int weekNum = getWeekNumber(startDay);
                weeklyHoursMap.merge(weekNum, 24 - startHour, Integer::sum);

                // 중간 날들
                for (int d = startDay + 1; d < actualEndDay; d++) {
                    weekNum = getWeekNumber(d);
                    weeklyHoursMap.merge(weekNum, 24, Integer::sum);
                }

                // 마지막 날 (월 내에 있을 때만)
                if (endDay <= lastDayOfMonth && endHour > 0) {
                    weekNum = getWeekNumber(endDay);
                    weeklyHoursMap.merge(weekNum, endHour, Integer::sum);
                }
            }
        }

        // 주차별로 15시간 이상인 경우 주휴수당 계산
        return weeklyHoursMap.entrySet().stream()
                .filter(e -> e.getValue() >= WEEKLY_THRESHOLD)
                .mapToLong(e -> {
                    int hours = Math.min(e.getValue(), WEEKLY_LIMIT);
                    return (long) ((hours / (double) WEEKLY_LIMIT) * 8 * wage);
                })
                .sum();
    }

    /**
     * ISO 주차 번호 반환 (월요일 시작 기준)
     */
    private int getWeekNumber(int day) {
        LocalDate date = LocalDate.of(year, month, day);
        WeekFields weekFields = WeekFields.of(Locale.KOREA); // 월요일 시작
        return date.get(weekFields.weekOfMonth());
    }
}