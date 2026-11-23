package com.ssafy.live.model.service;
import java.util.*;
import com.ssafy.live.model.dto.*;
public interface EnjoyTripService extends MemberService {
    Map<String, Long> tableCounts();
    TourResult queryTour(String action, Map<String, String> parameters);
    Map<String, Object> community();
    long savePost(long actorId, Map<String, Object> values);
    void deletePost(long actorId, long postId, String password);
    void addComment(long actorId, long postId, String content);
    void deleteComment(long actorId, long commentId);
    List<Map<String, Object>> plans(long actorId);
    long savePlan(long actorId, Map<String, Object> values);
    void deletePlan(long actorId, long planId);
    List<Map<String, Object>> records(long actorId);
    long saveRecord(long actorId, Map<String, Object> values);
    void deleteRecord(long actorId, long recordId);
}
