package com.payroll.calculator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkRecordRequest {

    @NotNull(message = "시작일은 필수입니다")
    @Min(1) @Max(31)
    private Integer startDay;

    @NotNull(message = "시작시간은 필수입니다")
    @Min(0) @Max(23)
    private Integer startHour;

    @NotNull(message = "종료일은 필수입니다")
    @Min(1) @Max(31)
    private Integer endDay;

    @NotNull(message = "종료시간은 필수입니다")
    @Min(0) @Max(23)
    private Integer endHour;
}