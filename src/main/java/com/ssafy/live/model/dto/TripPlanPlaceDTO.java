package com.ssafy.live.model.dto;
/** trip_plan_places 테이블의 한 행. */
public record TripPlanPlaceDTO(Long planPlaceId, Long planId, Integer attractionNo, Integer visitOrder, String memo) {
}
