# 💰 급여 계산기 구현 문제

## 문제 설명

근무 기록과 시급을 입력받아 급여를 계산하는 서비스를 구현하세요.

---

## 입력
```java
public class PayrollRequest {
    private List<WorkRecordRequest> records;  // 근무 기록 목록
    private Integer wage;                      // 시급 (원)
    private Integer year;                      // 년도
    private Integer month;                     // 월
}

public class WorkRecordRequest {
    private Integer startDay;    // 시작일 (1~31)
    private Integer startHour;   // 시작시간 (0~23)
    private Integer endDay;      // 종료일 (1~31)
    private Integer endHour;     // 종료시간 (0~23)
}
```

---

## 출력
```java
public class PayrollResponse {
    private double totalWorkHours;    // 총 근무시간
    private double overtimeHours;     // 연장근로시간
    private double nightHours;        // 야간근로시간
    private double holidayHours;      // 휴일근로시간
    
    private long basePay;             // 기본급
    private long overtimePay;         // 연장근로수당
    private long nightPay;            // 야간근로수당
    private long holidayPay;          // 휴일근로수당
    private long weeklyHolidayPay;    // 주휴수당
    
    private long totalPay;            // 총 지급액
}
```

---

## 계산 규칙

### 1. 기본급
- 모든 근무시간에 대해 시급 지급

### 2. 연장근로수당
- **조건**: 하루 8시간 초과
- **가산율**: 50%
- 같은 날 여러 번 출근해도 합산하여 판단

### 3. 야간근로수당
- **조건**: 22:00 ~ 06:00 근무
- **가산율**: 50%

### 4. 휴일근로수당
- **조건**: 일요일 근무
- **가산율**:
    - 8시간 이내: 50%
    - 8시간 초과: 100%

### 5. 주휴수당
- **조건**: 해당 주 총 근무시간 15시간 이상
- **계산**: `(주 근무시간 / 40) × 8 × 시급`
- 최대 40시간까지만 인정

---

## 주의사항

1. 근무가 여러 날에 걸칠 수 있음 (예: 10일 22시 ~ 13일 06시)
2. 하루에 여러 번 출근 가능 (예: 9~13시, 18~23시)
3. 수당은 중복 적용 (야간 + 연장 등)
4. 일요일 판단은 `java.time.DayOfWeek` 활용

---

## 예시

**입력**
```
records: [{startDay: 1, startHour: 9, endDay: 1, endHour: 18}]
wage: 10000
year: 2025
month: 1
```

**계산**
- 총 근무: 9시간
- 연장: 1시간 (9-8)
- 기본급: 9 × 10,000 = 90,000
- 연장수당: 1 × 10,000 × 0.5 = 5,000

**출력**
```
totalWorkHours: 9.0
overtimeHours: 1.0
basePay: 90000
overtimePay: 5000
totalPay: 95000
```

---

## 구현할 인터페이스
```java
public interface PayrollService {
    PayrollResponse calculate(PayrollRequest request);
}
```