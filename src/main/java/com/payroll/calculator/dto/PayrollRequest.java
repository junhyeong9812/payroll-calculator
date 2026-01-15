package com.payroll.calculator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PayrollRequest {

    @NotEmpty( message = "근무 기록은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<WorkRecordRequest> records;

    @NotNull(message = "시급은 필수입니다.")
    @Min(value = 1, message = "시급은 1 이상이어야 합니다.")
    private Integer wage;

    @NotNull(message = "년도는 필수입니다.")
    private Integer year;

    @NotNull(message = "월은 필수입니다.")
    @Min(1) @Max(12)
    private Integer month;
}