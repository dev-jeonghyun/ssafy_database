package com.ssafy.live.model.dto;
public record AttractionSearch(Integer sidoCode, Integer gugunCode, Integer contentTypeId, String keyword,
        Double longitude, Double latitude, Integer radius, int page, int size) {
}
