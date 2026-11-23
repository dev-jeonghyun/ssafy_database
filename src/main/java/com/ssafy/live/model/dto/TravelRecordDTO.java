package com.ssafy.live.model.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/** travel_records 테이블의 한 행. */
public record TravelRecordDTO(Long recordId, Long memberId, LocalDate visitDate, String recordType, String placeName,
        String address, BigDecimal latitude, BigDecimal longitude, String content, String imagePath,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
