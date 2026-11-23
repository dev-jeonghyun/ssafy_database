package com.ssafy.live.model.service;
import java.util.Map;
import com.ssafy.live.model.dto.TourResult;
/** 이전 TourService 호출부용 어댑터. 외부 API를 호출하지 않는다. */
public class BasicTourService implements TourService {
    private final EnjoyTripService service = new EnjoyTripServiceImpl();
    @Override
    public TourResult query(String action, Map<String, String> parameters, String unusedApiKey) {
        return service.queryTour(action, parameters);
    }
}
