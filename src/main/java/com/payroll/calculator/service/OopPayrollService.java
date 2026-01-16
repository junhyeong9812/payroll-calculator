package com.payroll.calculator.service;

import com.payroll.calculator.dto.PayrollRequest;
import com.payroll.calculator.dto.PayrollResponse;
import com.payroll.calculator.dto.WorkRecordRequest;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class OopPayrollService implements PayrollService {

    @Override
    public PayrollResponse calculate(PayrollRequest request) {

        return PayrollResponse.builder()
                .totalWorkHours(0)
                .overtimeHours(0)
                .nightHours(0)
                .holidayHours(0)
                .basePay(0)
                .overtimePay(0)
                .nightPay(0)
                .holidayPay(0)
                .weeklyHolidayPay(0)
                .totalPay(0)
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
        private final List<Work> works;
        private final BigDecimal wage;

        public Works(List<Work> works, BigDecimal wage) {
            this.works = works;
            this.wage = wage;
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

    public class Week {

    }

    public interface PayPolicy {
        BigDecimal calculate(Works works);
    }

    public class BasicPayPolicy implements PayPolicy {

        @Override
        public BigDecimal calculate(Works works) {
            return BigDecimal.ZERO;
        }
    }

    public class OverTimePayPolicy implements PayPolicy {

        @Override
        public BigDecimal calculate(Works works) {
            return BigDecimal.ZERO;
        }
    }

    public class NightPayPolicy implements PayPolicy {

        @Override
        public BigDecimal calculate(Works works) {
            return BigDecimal.ZERO;
        }
    }

    public class HolicayPayPolicy implements PayPolicy {

        @Override
        public BigDecimal calculate(Works works) {
            return BigDecimal.ZERO;
        }
    }
}
