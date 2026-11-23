package com.ssafy.live;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import javax.imageio.ImageIO;
import com.ssafy.live.model.dao.*;
import com.ssafy.live.model.dto.*;
import com.ssafy.live.model.service.*;
import com.ssafy.live.util.*;

/** 독립 ssafy_trip_test DB 전용 통합 검사. 실제 ssafy_trip에는 실행하지 않는다. */
public class DbIntegrationCheck {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok)
            throw new AssertionError(label);
        checks++;
    }
    private static void rejects(Runnable work, String label) {
        try {
            work.run();
            throw new AssertionError(label);
        } catch (IllegalArgumentException | AccessDeniedException | DataAccessException expected) {
            checks++;
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String url = System.getenv("ENJOYTRIP_TEST_DB_URL");
        if (url == null || !url.contains("/ssafy_trip_test"))
            throw new IllegalStateException("독립 ssafy_trip_test DB의 ENJOYTRIP_TEST_DB_URL을 설정하세요.");
        System.setProperty("db.url", url);
        System.setProperty("db.username", System.getenv().getOrDefault("ENJOYTRIP_TEST_DB_USERNAME", "root"));
        System.setProperty("db.password", System.getenv().getOrDefault("ENJOYTRIP_TEST_DB_PASSWORD", ""));
        EnjoyTripService service = new EnjoyTripServiceImpl();
        EnjoyTripDAO dao = new EnjoyTripDAOImpl();
        check(service.tableCounts().size() == 10, "10 tables");
        check(service.queryTour("regions", Map.of()).totalCount() == 17, "sido list");
        check(service.queryTour("categories", Map.of()).totalCount() == 8, "content types");
        var first = service.queryTour("search", Map.of("numOfRows", "5"));
        check(first.totalCount() == 50915 && first.items().size() == 5, "tourism dump and pagination");
        var second = service.queryTour("search", Map.of("numOfRows", "5", "pageNo", "2"));
        check(first.items().stream().noneMatch(
                a -> second.items().stream().anyMatch(b -> a.get("no").equals(b.get("no")))), "stable pages");
        var region = service.queryTour("search", Map.of("areaCode", "1", "sigunguCode", "1"));
        check(region.totalCount() > 0
                && region.items().stream().allMatch(a -> a.get("areacode").equals(1) && a.get("sigungucode").equals(1)),
                "composite region filter");
        check(service.queryTour("search", Map.of("keyword", "%' OR 1=1 --")).totalCount() == 0, "bound SQL input");
        var center = region.items().stream().filter(a -> a.get("mapx") != null && a.get("mapy") != null).findFirst()
                .orElseThrow();
        check(service
                .queryTour("nearby",
                        Map.of("mapX", center.get("mapx").toString(), "mapY", center.get("mapy").toString()))
                .totalCount() > 0, "distance query");
        check(service.login("ssafy", "1234") != null, "SQL PBKDF2 admin login");
        check(service.login("user000", "1234").getMno() == 0, "member zero preserved");
        check(service.login("sample-staff-guide", "1234") == null, "display-only author cannot log in");
        check(service.verifyRecoveryAnswer("ssafy", "ssafy"), "SQL recovery answer hash");
        check(!service.verifyRecoveryAnswer("ssafy", "wrong"), "wrong recovery answer rejected");
        Member one = new Member("검증 사용자1", "dbtest_one", "1234");
        one.setQuestion("질문");
        one.setAnswer("답변");
        service.registMember(one);
        Member two = new Member("검증 사용자2", "dbtest_two", "1234");
        two.setQuestion("질문");
        two.setAnswer("답변");
        service.registMember(two);
        long a = one.getMno(), b = two.getMno();
        check(service.login(one.getLoginId(), "1234") != null, "register/login");
        check(!service.isLoginIdAvailable(one.getLoginId()), "duplicate ID");
        Map<String, Object> post = new HashMap<>(
                Map.of("kind", "posts", "title", "통합검증 글", "body", "원문\n본문", "password", "4321"));
        long postId = service.savePost(a, post);
        post.put("id", Long.toString(postId));
        check(((List<?>) service.community().get("posts")).size() == 9, "post persisted");
        check(!service.community().toString().contains("pbkdf2_sha256"), "no password hashes in community response");
        rejects(() -> service.savePost(b, post), "other user's post update");
        post.put("password", "wrong");
        rejects(() -> service.savePost(a, post), "wrong post password");
        post.put("password", "4321");
        post.put("body", "수정 본문");
        service.savePost(a, post);
        service.addComment(b, postId, "다른 회원의 정상 댓글");
        CommentDTO comment = dao.findComments().stream().filter(c -> c.postId() == postId).findFirst().orElseThrow();
        rejects(() -> service.deleteComment(a, comment.commentId()), "other user's comment delete");
        service.deleteComment(b, comment.commentId());
        service.addComment(b, postId, "연쇄삭제 대상");
        rejects(() -> service.savePost(a, Map.of("kind", "notices", "title", "금지", "body", "본문")), "non-admin notice");
        long notice = service.savePost(100, Map.of("kind", "notices", "title", "관리자 검증", "body", "본문"));
        service.deletePost(100, notice, "");
        Map<String, Object> plan = new HashMap<>(Map.of("title", "검증 여행", "startDate", "2026-11-06", "endDate",
                "2026-11-07", "budget", 1000, "detail", "메모", "places",
                first.items().stream().limit(2).map(row -> Map.of("no", row.get("no"))).toList()));
        long planId = service.savePlan(a, plan);
        check(service.plans(a).size() == 1 && service.plans(b).isEmpty(), "private member plans");
        check(((List<?>) service.plans(a).getFirst().get("places")).size() == 2, "ordered plan places");
        rejects(() -> service.deletePlan(b, planId), "other user's plan delete");
        plan.put("endDate", "2026-11-05");
        rejects(() -> service.savePlan(a, plan), "invalid dates");
        long before = service.tableCounts().get("trip_plans");
        rejects(() -> dao.insertPlan(
                new TripPlanDTO(null, a, "rollback", LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, "", null, null),
                List.of(new TripPlanPlaceDTO(null, null, (Integer) first.items().getFirst().get("no"), 1, null),
                        new TripPlanPlaceDTO(null, null, Integer.MAX_VALUE, 2, null))),
                "invalid child rolls back whole plan");
        check(service.tableCounts().get("trip_plans") == before, "plan transaction rollback");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String image = ImageStore.save(a, bytes.toByteArray());
        check(ImageStore.exists(a, image), "photo persisted outside WAR");
        Map<String, Object> record = new HashMap<>(Map.of("date", "2026-11-06", "type", "관광", "place", "검증 장소",
                "address", "서울", "detail", "후기", "imagePath", image, "mapx", 127.0, "mapy", 37.5));
        long recordId = service.saveRecord(a, record);
        check(service.records(a).size() == 1 && service.records(b).isEmpty(), "private records");
        rejects(() -> service.saveRecord(b, record), "other user's photo cannot be attached");
        rejects(() -> service.deleteRecord(b, recordId), "other user's record delete");
        rejects(() -> ImageStore.file(a, "../secret.png"), "photo path traversal");
        service.deleteRecord(a, recordId);
        check(!ImageStore.exists(a, image), "record photo cleanup");
        service.deletePlan(a, planId);
        check(dao.findPlanPlaces(planId).isEmpty(), "plan cascade delete");
        service.deletePost(a, postId, "4321");
        check(dao.findComments().stream().noneMatch(c -> c.postId() == postId), "post comment cascade");
        Member update = new Member("이름 변경", one.getLoginId(), "5678");
        update.setRole("ADMIN");
        update.setQuestion("질문");
        update.setAnswer("");
        service.memberModify(update, one.getLoginId(), "1234");
        check(service.login(one.getLoginId(), "5678").getRole().equals("USER"), "member cannot escalate role");
        check(service.verifyRecoveryAnswer(one.getLoginId(), "답변"), "unchanged recovery hash preserved");
        check(service.resetPassword(one.getLoginId(), "9012") && service.login(one.getLoginId(), "9012") != null,
                "password reset");
        check(service.memberDelete(one.getLoginId(), one.getLoginId(), "9012"), "withdrawal");
        check(service.login(one.getLoginId(), "9012") == null && service.findByLoginId(one.getLoginId()) == null,
                "withdrawn login rejected");
        System.out.println("DB INTEGRATION PASS: " + checks + " checks");
    }
}
