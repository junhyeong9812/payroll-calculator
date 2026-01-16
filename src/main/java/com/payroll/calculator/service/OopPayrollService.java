package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class OopPayrollService implements PayrollService {

    @Override
    public PayrollResponse calculate(PayrollRequest request) {
        Works works = Works.of(request);

        // 1. 각 정책의 결과(시간+금액)를 받아옴
        PolicyResult basic = new BasicPayPolicy().calculate(works);
        PolicyResult overtime = new OverTimePayPolicy().calculate(works);
        PolicyResult night = new NightPayPolicy().calculate(works);
        PolicyResult holiday = new HolidayPayPolicy().calculate(works);
        PolicyResult weekly = new WeeklyHolidayPayPolicy().calculate(works);

        // 2. 총액 계산
        BigDecimal totalPay = basic.pay().add(overtime.pay()).add(night.pay())
                .add(holiday.pay()).add(weekly.pay());

        // 3. 빌더에 매핑 (누락 없이 깔끔하게!)
        return PayrollResponse.builder()
                .totalWorkHours(works.count())
                .basePay(basic.pay().longValue())
                .overtimeHours((int) overtime.hours())
                .overtimePay(overtime.pay().longValue())
                .nightHours((int) night.hours())
                .nightPay(night.pay().longValue())
                .holidayHours((int) holiday.hours())
                .holidayPay(holiday.pay().longValue())
                .weeklyHolidayPay(weekly.pay().longValue())
                .totalPay(totalPay.longValue())
                .build();
    }
    /**
     *  시급을 계산한다.
     * 결국 시간의 기준은 1시간을 기준으로 모든게 나뉜다.
     * 이때 하나의 시간을 바라보고 각각의 기본급이라는 객체
     * 연장근로수당이라는 객체
     * 야간근로수당이라는 객체
     * 휴일 근로수당이라는 객체
     * 주휴 수당이라는 객체로 나눌 수 있나?
     * 하지만 이렇다면 각각의 시간을 가져와서 해당 시간에 대해서
     * 각각 연산해야된다.
     * 그렇다면 시간의 축을 각각 쓸 지
     * 이 시간이라는 축이 베이스가 되어 난 어떤 상태들인지 판단하게 할 지.
     * 이때 바라볼 관점을 보자.
     * 시간이라는 축을 기본으로 한다면 결국 각 시간은 내가 위의 5가지 상태의 유무만 판단하고 이러한 워커들의 상태를
     * 통합하면 된다.
     * 하지만 이때 위와 같이 정책이나 계산에 대한 상태를 각각 가지면 하나의 루트가 아닌 각기 다른 루트들이 생기며
     * 이러한 루트는 파생됬을때 1개에서 파생보다 5개에서 파생되기에 루트파생이 더 커지고 유지보수가 곤란해진다.
     **/

    public static class Works {
        HashMap<LocalDate, Integer> workTime = new HashMap<>();
        private final List<Work> works;
        private final BigDecimal wage;

        public Works(List<Work> works, BigDecimal wage) {
            for (Work work : works) {
                workTime.merge(work.getDate(),1,Integer::sum);
            }
            this.works = works;
            this.wage = wage;
        }

        public HashMap<LocalDate, Integer> getWorkTime() {
            return workTime;
        }

        public int count() {
            return works.size();
        }

//        public Map<LocalDate, Integer> getWeeklyWorkHours() {
//            Map<LocalDate, Integer> weeklyMap = new HashMap<>();
//
//            for (Work work : works) {
//                LocalDate date = work.getDate();
//                int dayValue = date.getDayOfWeek().getValue();
//                LocalDate startOfThisWeek = date.minusDays(dayValue - 1);
//
//                weeklyMap.merge(startOfThisWeek, 1, Integer::sum);
//            }
//            return weeklyMap;
//        }

        public Map<Integer, Integer> getWeeklyWorkHours() {
            WeekFields weekFields = WeekFields.of(Locale.KOREA);
            Map<Integer, Integer> weeklyMap = new HashMap<>();

            for (Work work: works) {
                int weekOfYear = work.getDate().get(weekFields.weekBasedYear());

                weeklyMap.merge(weekOfYear, 1, Integer::sum);
            }
            return weeklyMap;
        }

//        public static Works of(PayrollRequest payrollRequest) {
//            List<Work> works = payrollRequest.getRecords().stream()
//                    .flatMap(record -> splitRecord(payrollRequest.getYear(), payrollRequest.getMonth(), record).stream())
//                    .toList();
//
//            return new Works(works,BigDecimal.valueOf(payrollRequest.getWage()));
//        }
//
//        private static List<Work> splitRecord(Integer year, Integer month,WorkRecordRequest request) {
//            LocalDateTime startTime = LocalDateTime.of(year,month, request.getStartDay(), request.getStartHour(), 0);
//            LocalDateTime endTime = LocalDateTime.of(year,month, request.getEndDay(), request.getEndHour(), 0);
//            List<Work> workingTimes = new ArrayList<>();
//            for (LocalDateTime workingTime = startTime; workingTime.isBefore(endTime); workingTime = workingTime.plusHours(1) ) {
//                workingTimes.add(Work.of(workingTime));
//            }
//            return workingTimes;
//        }
        /**
         * 현재 위의 코드는 list를 2번 만들어서 데이터에 대한 생성을 2번하여 메모리가 오버될 가능성이 큰 코드로
         * 아래와 같이 개선하면  애초에 반환을 스트림에 넣어서 반환하고 그 스트림을 바로 활용하도록 할 수 있다면?
         * 즉 기존의 list로 넣어 반환하면 flatMap에서 리스트에서 꺼내서 다시 꺼내서 스트림에 넣어줘야된다.
         * 그렇기에 애초에 스트림으로 반환하도록 하
         */
        public static Works of(PayrollRequest payrollRequest) {
            List<Work> works = payrollRequest.getRecords().stream()
                    .flatMap(record -> splitRecord(payrollRequest.getYear(), payrollRequest.getMonth(), record))
                    .toList();

            return new Works(works,BigDecimal.valueOf(payrollRequest.getWage()));
        }
        private static Stream<Work> splitRecord(Integer year, Integer month, WorkRecordRequest request) {
            LocalDateTime start = LocalDateTime.of(year, month, request.getStartDay(), request.getStartHour() ,0);
            LocalDateTime end = LocalDateTime.of(year, month, request.getEndDay(), request.getEndHour(),0);

            return Stream.iterate(start, t -> t.isBefore(end), t -> t.plusHours(1))
                    .map(Work::of);
        }
    }

    public static class Work {
        private final LocalDateTime dateTime;

        public boolean isNight() {
            int hour = dateTime.getHour();
            return hour >= 22 || hour < 6;
        }

        public boolean isHoliday() {
            return dateTime.getDayOfWeek() == DayOfWeek.SUNDAY;
        }

        public LocalDate getDate() {
            return dateTime.toLocalDate();
        }

        private Work(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        public static Work of(Integer year, Integer month, Integer day, Integer hour) {
            return new Work(LocalDateTime.of(year, month, day, hour, 0));
        }

        public static Work of(LocalDateTime time) {
            return new Work(time);
        }
    }

    public interface PayPolicy {
        PolicyResult calculate(Works works);
    }

    public class BasicPayPolicy implements PayPolicy {

        @Override
        public PolicyResult calculate(Works works) {
            int totalHours = works.count();
            BigDecimal pay = works.wage.multiply(BigDecimal.valueOf(totalHours));
            return PolicyResult.of(pay, totalHours);
        }
    }

    public class OverTimePayPolicy implements PayPolicy {

        @Override
        public PolicyResult calculate(Works works) {
            HashMap<LocalDate, Integer> workTime = new HashMap<>();

            for (Work work : works.works) {
                workTime.merge(work.getDate(),1,Integer::sum);
            }

            // for문을 통한 연장 근무 찾기
//            int totalOvertimeHours = 0;
//            for (int hours: workTime.values()) {
//                if (hours > 8) {
//                    totalOvertimeHours += (hours - 8);
//                }
//            }

            // stream을 활용한 연장근무 탐색
            int totalOvertimeHours= workTime.values().stream()
                    .filter(hours -> hours >8)
                    .mapToInt(hours -> hours - 8)
                    .sum();
            BigDecimal pay = works.wage.multiply(BigDecimal.valueOf(totalOvertimeHours))
                    .multiply(BigDecimal.valueOf(0.5));
            return PolicyResult.of(pay, totalOvertimeHours);
        }
    }

    public class NightPayPolicy implements PayPolicy {

        @Override
        public PolicyResult calculate(Works works) {
            long totalNightHours = works.works.stream()
                    .filter(Work::isNight)
                    .count();
            BigDecimal pay = works.wage.multiply(BigDecimal.valueOf(totalNightHours))
                    .multiply(BigDecimal.valueOf(0.5));
            return PolicyResult.of(pay, (double) totalNightHours);
        }
    }

    public class HolidayPayPolicy implements PayPolicy {

        @Override
        public PolicyResult calculate(Works works) {
            HashMap<LocalDate, Integer> holidayWorkTime= new HashMap<>();
//            List<Work> HolidayWork = works.works.stream()
//                    .filter(Work::isHoliday)
//                    .toList();
//            for (Work work : HolidayWork) {
//                holidayWorkTime.merge(work.getDate(),1,Integer::sum);
//            }
            //  위리스트 코드를 스트림으로 만든다면?
            works.works.stream()
                    .filter(Work::isHoliday)
                    .forEach(work -> holidayWorkTime.merge(work.getDate(),1,Integer::sum));

            long totalHolidayHoursOver8 = holidayWorkTime.values().stream()
                    .filter(hours -> hours > 8)
                    .mapToInt(hours -> hours - 8)
                    .sum();
            long totalHolidayHoursUnder8 = holidayWorkTime.values().stream()
                    .mapToInt(hours -> Math.min(hours,8))
                    .sum();

            BigDecimal pay = works.wage.multiply(BigDecimal.valueOf(totalHolidayHoursUnder8)).multiply(BigDecimal.valueOf(0.5))
                    .add(works.wage.multiply(BigDecimal.valueOf(totalHolidayHoursOver8))); // 100% 가산

            return PolicyResult.of(pay, totalHolidayHoursUnder8 + totalHolidayHoursOver8);
        }
    }

    public class WeeklyHolidayPayPolicy implements PayPolicy {

        @Override
        public PolicyResult calculate(Works works) {
            HashMap<LocalDate, Integer> weekly = new HashMap<>();
            for(Work work: works.works) {
                LocalDate date = work.getDate();
                int dayValue= date.getDayOfWeek().getValue();
                LocalDate startOfThisWeek = date.minusDays(dayValue - 1);
                weekly.merge(startOfThisWeek, 1,Integer::sum);
            }
//            Map<LocalDate, Integer> weekly = works.getWeeklyWorkHours();

            return weekly.values().stream()
                    .filter(hours -> hours >= 15)
                    .map(hours -> {
                        // 인정되는 주휴 시간 계산 (기존 변수명 유지)
                        double weeklyHolidayHours = (Math.min(hours, 40) / 40.0) * 8.0;
                        BigDecimal pay = works.wage.multiply(BigDecimal.valueOf(weeklyHolidayHours));

                        // 중간 결과를 PolicyResult에 임시 저장
                        return PolicyResult.of(pay, weeklyHolidayHours);
                    })
                    .reduce(PolicyResult.of(BigDecimal.ZERO, 0), (a, b) ->
                            PolicyResult.of(a.pay().add(b.pay()), a.hours() + b.hours())
                    );
        }
    }

    public record PolicyResult(BigDecimal pay, double hours) {
        public static PolicyResult of(BigDecimal pay, double hours) {
            return new PolicyResult(pay, hours);
        }
    }
}
