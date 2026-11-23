package com.ssafy.live.model.service;

import java.io.IOException;
import java.util.Map;

import com.ssafy.live.model.dto.TourResult;

public interface TourService {
    TourResult query(String action, Map<String, String> parameters, String apiKey)
            throws IOException, InterruptedException;
}
