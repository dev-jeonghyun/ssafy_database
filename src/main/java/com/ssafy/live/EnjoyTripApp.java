package com.ssafy.live;
import java.util.Map;
import com.ssafy.live.model.service.*;
/** STS: Run As -> Java Application. 조회만 수행하며 초기 데이터를 변경하지 않는다. */
public class EnjoyTripApp {
    public static void main(String[] args) {
        EnjoyTripService service = new EnjoyTripServiceImpl();
        try {
            System.out.println("=== BE_06 / MySQL 연결 및 테이블별 데이터 수 ===");
            service.tableCounts().forEach((table, count) -> System.out.println(table + " : " + count));
            System.out.println("=== 시도 목록 ===");
            service.queryTour("regions", Map.of()).items().forEach(System.out::println);
            System.out.println("=== 관광지 첫 5건 ===");
            service.queryTour("search", Map.of("numOfRows", "5")).items().forEach(
                    row -> System.out.println(row.get("no") + " / " + row.get("title") + " / " + row.get("addr1")));
            System.out.println("=== 공지/게시글 조회 ===");
            Map<String, Object> community = service.community();
            System.out.println("공지=" + ((java.util.List<?>) community.get("notices")).size() + ", 일반글="
                    + ((java.util.List<?>) community.get("posts")).size());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
