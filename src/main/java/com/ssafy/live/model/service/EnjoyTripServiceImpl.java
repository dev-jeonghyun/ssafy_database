package com.ssafy.live.model.service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.ssafy.live.model.dao.*;
import com.ssafy.live.model.dto.*;
import com.ssafy.live.util.*;

/** 입력/권한 검증은 Service, SQL 실행은 DAO에서 처리한다. */
public class EnjoyTripServiceImpl implements EnjoyTripService {
    private final EnjoyTripDAO dao;
    public EnjoyTripServiceImpl() {
        this(new EnjoyTripDAOImpl());
    }
    public EnjoyTripServiceImpl(EnjoyTripDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }
    public static Map<String, Object> object(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2)
            result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
    private static String required(Object value, String label, int max) {
        String result = text(value).trim();
        if (result.isEmpty() || result.length() > max)
            throw new IllegalArgumentException(label + "은(는) 1~" + max + "자로 입력해주세요.");
        return result;
    }
    private static String optional(Object value, String label, int max) {
        String result = text(value).trim();
        if (result.length() > max)
            throw new IllegalArgumentException(label + "의 최대 길이를 초과했습니다.");
        return result;
    }
    private static long id(Object value) {
        try {
            long result = Long.parseLong(text(value));
            if (result < 0)
                throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("번호가 올바르지 않습니다.");
        }
    }
    private static int number(Map<String, String> p, String key, int fallback, int min, int max) {
        try {
            String value = p.get(key);
            int result = value == null || value.isBlank() ? fallback : Integer.parseInt(value);
            if (result < min || result > max)
                throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + "의 범위가 올바르지 않습니다.");
        }
    }
    private static Integer code(Map<String, String> p, String key) {
        return p.get(key) == null || p.get(key).isBlank() ? null : number(p, key, 0, 1, 1000000);
    }
    private static double coordinate(String value, double min, double max) {
        try {
            double result = Double.parseDouble(value);
            if (!Double.isFinite(result) || result < min || result > max)
                throw new NumberFormatException();
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("좌표가 올바르지 않습니다.");
        }
    }
    private static void password(String password) {
        if (password == null || password.length() < 4 || password.length() > 128)
            throw new IllegalArgumentException("비밀번호는 4~128자로 입력해주세요.");
    }
    private MemberDTO active(long id) {
        MemberDTO m = dao.findMember(id);
        if (m == null || !Boolean.TRUE.equals(m.isLoginEnabled()))
            throw new AccessDeniedException("로그인이 만료되었습니다. 다시 로그인해주세요.");
        return m;
    }
    private MemberDTO active(String id) {
        MemberDTO m = dao.findMember(id);
        return m != null && Boolean.TRUE.equals(m.isLoginEnabled()) ? m : null;
    }
    private static Member publicMember(MemberDTO d) {
        if (d == null)
            return null;
        return new Member(d.memberId(), d.name(), d.loginId(), null, d.role(), d.recoveryQuestion(), null);
    }
    private static String time(LocalDateTime d) {
        return d == null ? "" : d.toString() + "+09:00";
    }
    @Override
    public Map<String, Long> tableCounts() {
        return dao.tableCounts();
    }
    @Override
    public TourResult queryTour(String action, Map<String, String> p) {
        if (action.equals("regions")) {
            Integer sido = code(p, "areaCode");
            List<Map<String, Object>> rows = new ArrayList<>();
            if (sido == null)
                for (SidoDTO d : dao.findSidos())
                    rows.add(object("code", d.sidoCode().toString(), "name", d.sidoName(), "key",
                            d.sidoCode().toString(), "label", d.sidoName()));
            else
                for (GugunDTO d : dao.findGuguns(sido))
                    rows.add(object("code", d.gugunCode().toString(), "name", d.gugunName(), "key",
                            d.gugunCode().toString(), "label", d.gugunName()));
            return new TourResult(rows, rows.size(), 1, rows.size());
        }
        if (action.equals("categories")) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ContentTypeDTO d : dao.findContentTypes())
                rows.add(object("code", d.contentTypeId().toString(), "name", d.contentTypeName(), "key",
                        d.contentTypeId().toString(), "label", d.contentTypeName()));
            return new TourResult(rows, rows.size(), 1, rows.size());
        }
        if (action.equals("images")) {
            int content = number(p, "contentId", 0, 1, Integer.MAX_VALUE);
            AttractionDTO d = dao.findAttractionByContentId(content);
            List<Map<String, Object>> rows = new ArrayList<>();
            if (d != null) {
                for (String image : List.of(text(d.firstImage1()), text(d.firstImage2())))
                    if (!image.isBlank() && rows.stream().noneMatch(r -> image.equals(r.get("originimgurl"))))
                        rows.add(object("originimgurl", image, "smallimageurl", image, "imgname", d.title()));
            }
            return new TourResult(rows, rows.size(), 1, Math.max(rows.size(), 1));
        }
        if (!Set.of("search", "nearby").contains(action))
            throw new IllegalArgumentException("지원하지 않는 관광 조회입니다.");
        Integer sido = code(p, "areaCode"), gugun = code(p, "sigunguCode"), type = code(p, "contentTypeId");
        if (gugun != null && sido == null)
            throw new IllegalArgumentException("시도를 먼저 선택해주세요.");
        Double x = null, y = null;
        Integer radius = null;
        if (action.equals("nearby")) {
            x = coordinate(p.get("mapX"), -180, 180);
            y = coordinate(p.get("mapY"), -90, 90);
            radius = number(p, "radius", 2000, 1, 20000);
        }
        int page = number(p, "pageNo", 1, 1, 1000000), size = number(p, "numOfRows", 20, 1, 100);
        AttractionSearch search = new AttractionSearch(sido, gugun, type, optional(p.get("keyword"), "검색어", 100), x, y,
                radius, page, size);
        int total = dao.countAttractions(search);
        return new TourResult(dao.findAttractions(search).stream().map(this::attraction).toList(), total, page, size);
    }
    private Map<String, Object> attraction(AttractionDTO d) {
        return object("no", d.no(), "contentid", text(d.contentId()), "contenttypeid", text(d.contentTypeId()), "title",
                text(d.title()), "areacode", d.areaCode(), "sigungucode", d.siGunGuCode(), "firstimage",
                text(d.firstImage1()), "firstimage2", text(d.firstImage2()), "mapx", d.longitude(), "mapy",
                d.latitude(), "addr1", text(d.addr1()), "addr2", text(d.addr2()), "tel", text(d.tel()), "homepage",
                text(d.homepage()), "overview", text(d.overview()));
    }

    @Override
    public int registMember(Member m) {
        if (m.getLoginId() == null || !m.getLoginId().matches("[a-z0-9_]{4,20}"))
            throw new IllegalArgumentException("아이디는 소문자 영문·숫자·밑줄 4~20자로 입력해주세요.");
        if (dao.findMember(m.getLoginId()) != null)
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        password(m.getPassword());
        String name = required(m.getName(), "이름", 40), question = required(m.getQuestion(), "복구 질문", 100),
                answer = required(m.getAnswer(), "복구 답변", 100);
        long id = dao.insertMember(new MemberDTO(null, m.getLoginId(), PasswordUtil.hash(m.getPassword()), name, true,
                "USER", question, PasswordUtil.hash(answer), null, null));
        m.setMno(id);
        m.setRole("USER");
        return 1;
    }
    @Override
    public void reset() {
        throw new IllegalArgumentException("DB 초기화는 별도 SQL로 관리합니다. 이 화면에서는 회원 전체를 초기화하지 않습니다.");
    }
    @Override
    public Page<Member> search(SearchCondition input) {
        SearchCondition c = input == null ? new SearchCondition() : input;
        c.setItemsPerPage(Math.min(c.getItemsPerPage(), 100));
        int total = dao.countMembers(c);
        c.setCurrentPage(
                Math.min(c.getCurrentPage(), Math.max(1, (total + c.getItemsPerPage() - 1) / c.getItemsPerPage())));
        return new Page<>(c, total, dao.findMembers(c).stream().map(EnjoyTripServiceImpl::publicMember).toList());
    }
    @Override
    public Member login(String loginId, String password) {
        MemberDTO d = active(loginId);
        return d != null && PasswordUtil.verify(password, d.passwordHash()) ? publicMember(d) : null;
    }
    @Override
    public Member findByLoginId(String loginId) {
        return publicMember(active(loginId));
    }
    @Override
    public boolean isLoginIdAvailable(String loginId) {
        return loginId != null && loginId.matches("[a-z0-9_]{4,20}") && dao.findMember(loginId) == null;
    }
    private MemberDTO manager(String target, String actor, String currentPassword) {
        MemberDTO d = active(actor);
        if (d == null || !PasswordUtil.verify(currentPassword, d.passwordHash()))
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        if (!d.loginId().equals(target) && !d.role().equals("ADMIN"))
            throw new AccessDeniedException("해당 회원을 변경할 권한이 없습니다.");
        return d;
    }
    @Override
    public Member memberModify(Member input, String actorId, String currentPassword) {
        MemberDTO actor = manager(input.getLoginId(), actorId, currentPassword), old = active(input.getLoginId());
        if (old == null)
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        String newPassword = old.passwordHash();
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            password(input.getPassword());
            newPassword = PasswordUtil.hash(input.getPassword());
        }
        String question = required(input.getQuestion(), "복구 질문", 100), answer = old.recoveryAnswerHash();
        if (input.getAnswer() != null && !input.getAnswer().isBlank())
            answer = PasswordUtil.hash(required(input.getAnswer(), "복구 답변", 100));
        else if (!question.equals(old.recoveryQuestion()))
            throw new IllegalArgumentException("복구 질문을 바꾸면 새 답변도 입력해주세요.");
        String role = actor.role().equals("ADMIN") ? text(input.getRole()) : old.role();
        if (!Set.of("USER", "ADMIN").contains(role))
            throw new IllegalArgumentException("권한이 올바르지 않습니다.");
        dao.updateMember(new MemberDTO(old.memberId(), old.loginId(), newPassword, required(input.getName(), "이름", 40),
                true, role, question, answer, old.createdAt(), old.updatedAt()));
        return findByLoginId(old.loginId());
    }
    @Override
    public boolean memberDelete(String loginId, String actorId, String currentPassword) {
        manager(loginId, actorId, currentPassword);
        MemberDTO old = active(loginId);
        if (old == null)
            return false;
        if (old.role().equals("ADMIN"))
            throw new IllegalArgumentException("관리자 계정은 탈퇴할 수 없습니다.");
        return dao.deactivateMember(loginId);
    }
    @Override
    public String getRecoveryQuestion(String loginId) {
        MemberDTO d = active(loginId);
        if (d == null || d.recoveryQuestion() == null)
            throw new IllegalArgumentException("복구 정보를 찾을 수 없습니다.");
        return d.recoveryQuestion();
    }
    @Override
    public boolean verifyRecoveryAnswer(String loginId, String answer) {
        MemberDTO d = active(loginId);
        return d != null && answer != null && PasswordUtil.verify(answer.trim(), d.recoveryAnswerHash());
    }
    @Override
    public boolean resetPassword(String loginId, String password) {
        password(password);
        return dao.updatePassword(loginId, PasswordUtil.hash(password));
    }

    @Override
    public Map<String, Object> community() {
        List<CommentDTO> comments = dao.findComments();
        List<Map<String, Object>> notices = new ArrayList<>(), posts = new ArrayList<>();
        for (PostDTO d : dao.findPosts()) {
            List<Map<String, Object>> replies = comments
                    .stream().filter(c -> c.postId().equals(d.postId())).map(c -> object("id", c.commentId().toString(),
                            "authorId", c.memberId().toString(), "body", c.content(), "createdAt", time(c.createdAt())))
                    .toList();
            Map<String, Object> post = object("id", d.postId().toString(), "authorId", d.memberId().toString(), "title",
                    d.title(), "body", d.content(), "createdAt", time(d.createdAt()), "updatedAt", time(d.updatedAt()),
                    "comments", replies);
            (d.postType().equals("NOTICE") ? notices : posts).add(post);
        }
        List<Map<String, Object>> names = dao.findAuthorNames().entrySet().stream()
                .map(e -> object("id", e.getKey().toString(), "name", e.getValue())).toList();
        return object("users", names, "notices", notices, "posts", posts);
    }
    private PostDTO manageable(long actorId, long postId, String password) {
        MemberDTO actor = active(actorId);
        PostDTO post = dao.findPost(postId);
        if (post == null)
            throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
        if (post.postType().equals("NOTICE")) {
            if (!actor.role().equals("ADMIN"))
                throw new AccessDeniedException("공지사항은 관리자만 변경할 수 있습니다.");
        } else if (!post.memberId().equals(actorId) || !PasswordUtil.verify(password, post.postPasswordHash()))
            throw new AccessDeniedException("작성자 계정과 글 비밀번호를 확인해주세요.");
        return post;
    }
    @Override
    public long savePost(long actorId, Map<String, Object> values) {
        MemberDTO actor = active(actorId);
        String type = text(values.get("kind")).equals("notices") ? "NOTICE" : "GENERAL";
        String title = required(values.get("title"), "제목", 200), body = required(values.get("body"), "내용", 10000),
                password = text(values.get("password"));
        if (!text(values.get("id")).isBlank()) {
            PostDTO old = manageable(actorId, id(values.get("id")), password);
            if (!type.equals(old.postType()))
                throw new IllegalArgumentException("게시글 유형은 변경할 수 없습니다.");
            dao.updatePost(new PostDTO(old.postId(), old.memberId(), old.postType(), title, old.postPasswordHash(),
                    body, old.createdAt(), old.updatedAt()));
            return old.postId();
        }
        if (type.equals("NOTICE") && !actor.role().equals("ADMIN"))
            throw new AccessDeniedException("공지사항은 관리자만 작성할 수 있습니다.");
        if (type.equals("GENERAL"))
            password(password);
        return dao.insertPost(new PostDTO(null, actorId, type, title,
                type.equals("GENERAL") ? PasswordUtil.hash(password) : null, body, null, null));
    }
    @Override
    public void deletePost(long actor, long post, String password) {
        manageable(actor, post, password);
        if (!dao.deletePost(post))
            throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
    }
    @Override
    public void addComment(long actor, long postId, String content) {
        active(actor);
        PostDTO post = dao.findPost(postId);
        if (post == null || !post.postType().equals("GENERAL"))
            throw new IllegalArgumentException("댓글을 작성할 일반 게시글이 없습니다.");
        dao.insertComment(new CommentDTO(null, postId, actor, required(content, "댓글", 2000), null, null));
    }
    @Override
    public void deleteComment(long actor, long id) {
        active(actor);
        if (!dao.deleteComment(id, actor))
            throw new AccessDeniedException("본인이 작성한 댓글만 삭제할 수 있습니다.");
    }

    @Override
    public List<Map<String, Object>> plans(long actor) {
        active(actor);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TripPlanDTO d : dao.findPlans(actor)) {
            List<Map<String, Object>> places = new ArrayList<>();
            for (TripPlanPlaceDTO p : dao.findPlanPlaces(d.planId())) {
                AttractionDTO a = dao.findAttraction(p.attractionNo());
                Map<String, Object> row = attraction(a);
                row.put("address", text(a.addr1()));
                row.put("visitOrder", p.visitOrder());
                places.add(row);
            }
            result.add(object("id", d.planId().toString(), "title", d.title(), "startDate", d.startDate().toString(),
                    "endDate", d.endDate().toString(), "budget", d.budget(), "detail", d.memo(), "places", places));
        }
        return result;
    }
    @Override
    public long savePlan(long actor, Map<String, Object> values) {
        active(actor);
        LocalDate start, end;
        BigDecimal budget;
        try {
            start = LocalDate.parse(text(values.get("startDate")));
            end = LocalDate.parse(text(values.get("endDate")));
            budget = new BigDecimal(text(values.getOrDefault("budget", 0)));
        } catch (Exception e) {
            throw new IllegalArgumentException("여행 기간과 예산을 확인해주세요.");
        }
        if (end.isBefore(start) || budget.signum() < 0 || budget.scale() > 2
                || budget.compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("여행 기간 또는 예산이 올바르지 않습니다.");
        if (!(values.get("places") instanceof List<?> input) || input.isEmpty() || input.size() > 100)
            throw new IllegalArgumentException("여행지를 1~100곳 선택해주세요.");
        List<TripPlanPlaceDTO> places = new ArrayList<>();
        for (Object value : input) {
            if (!(value instanceof Map<?, ?> row))
                throw new IllegalArgumentException("여행지 형식이 올바르지 않습니다.");
            AttractionDTO a;
            try {
                a = row.get("no") != null
                        ? dao.findAttraction(Math.toIntExact(id(row.get("no"))))
                        : dao.findAttractionByContentId(Math.toIntExact(id(row.get("contentid"))));
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("관광지 번호가 올바르지 않습니다.");
            }
            if (a == null)
                throw new IllegalArgumentException("DB에 없는 관광지가 포함되어 있습니다.");
            places.add(new TripPlanPlaceDTO(null, null, a.no(), places.size() + 1, null));
        }
        return dao.insertPlan(new TripPlanDTO(null, actor, required(values.get("title"), "계획 제목", 200), start, end,
                budget, optional(values.get("detail"), "메모", 10000), null, null), places);
    }
    @Override
    public void deletePlan(long actor, long plan) {
        active(actor);
        if (!dao.deletePlan(plan, actor))
            throw new AccessDeniedException("본인의 여행 계획만 삭제할 수 있습니다.");
    }
    @Override
    public List<Map<String, Object>> records(long actor) {
        active(actor);
        return dao.findRecords(actor).stream()
                .map(d -> object("id", d.recordId().toString(), "date", d.visitDate().toString(), "type",
                        d.recordType(), "place", d.placeName(), "address", d.address(), "detail", d.content(),
                        "imagePath", d.imagePath(), "mapx", d.longitude(), "mapy", d.latitude()))
                .toList();
    }
    @Override
    public long saveRecord(long actor, Map<String, Object> values) {
        active(actor);
        LocalDate date;
        try {
            date = LocalDate.parse(text(values.get("date")));
        } catch (Exception e) {
            throw new IllegalArgumentException("방문일을 확인해주세요.");
        }
        BigDecimal lat = null, lon = null;
        if (!text(values.get("mapx")).isBlank() || !text(values.get("mapy")).isBlank()) {
            lon = BigDecimal.valueOf(coordinate(text(values.get("mapx")), -180, 180));
            lat = BigDecimal.valueOf(coordinate(text(values.get("mapy")), -90, 90));
        }
        String image = required(values.get("imagePath"), "사진", 100);
        if (!ImageStore.exists(actor, image))
            throw new IllegalArgumentException("업로드한 본인의 사진을 선택해주세요.");
        return dao.insertRecord(new TravelRecordDTO(null, actor, date, required(values.get("type"), "기록 유형", 50),
                required(values.get("place"), "장소명", 500), required(values.get("address"), "주소", 500), lat, lon,
                optional(values.get("detail"), "후기", 10000), image, null, null));
    }
    @Override
    public void deleteRecord(long actor, long record) {
        active(actor);
        TravelRecordDTO target = dao.findRecords(actor).stream().filter(r -> r.recordId() == record).findFirst()
                .orElseThrow(() -> new AccessDeniedException("본인의 여행 기록만 삭제할 수 있습니다."));
        if (!dao.deleteRecord(record, actor))
            throw new NoSuchElementException("여행 기록을 찾을 수 없습니다.");
        if (dao.findRecords(actor).stream().noneMatch(r -> Objects.equals(r.imagePath(), target.imagePath())))
            ImageStore.delete(actor, target.imagePath());
    }
}
