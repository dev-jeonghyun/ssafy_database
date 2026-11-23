package com.ssafy.live.model.dto;

import java.util.List;
import java.util.Map;

public record TourResult(List<Map<String, Object>> items, int totalCount, int pageNo, int numOfRows) {
}
