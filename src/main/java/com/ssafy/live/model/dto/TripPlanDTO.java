package com.ssafy.live.model.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/** trip_plans 테이블의 한 행. */
public record TripPlanDTO(Long planId, Long memberId, String title, LocalDate startDate, LocalDate endDate,
        BigDecimal budget, String memo, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
