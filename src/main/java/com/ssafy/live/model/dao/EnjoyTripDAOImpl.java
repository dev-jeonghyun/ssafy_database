package com.ssafy.live.model.dao;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.ssafy.live.model.dto.*;
import com.ssafy.live.util.*;

/** SQL 식별자는 코드에 고정하고 사용자 입력값은 PreparedStatement로 바인딩한다. */
public class EnjoyTripDAOImpl implements EnjoyTripDAO {
    private static final List<String> TABLES = List.of("sidos", "guguns", "contenttypes", "attractions", "members",
            "trip_plans", "trip_plan_places", "posts", "comments", "travel_records");
    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
    private static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++)
            statement.setObject(i + 1, args[i]);
    }
    private static <T> T map(Class<T> type, ResultSet rs) throws ReflectiveOperationException, SQLException {
        RecordComponent[] fields = type.getRecordComponents();
        Object[] values = new Object[fields.length];
        Class<?>[] types = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; i++) {
            types[i] = fields[i].getType();
            Object value = rs.getObject(snake(fields[i].getName()));
            if (value != null) {
                if (types[i] == Long.class)
                    value = ((Number) value).longValue();
                else if (types[i] == Integer.class)
                    value = ((Number) value).intValue();
                else if (types[i] == Boolean.class)
                    value = value instanceof Boolean ? value : ((Number) value).intValue() != 0;
                else if (types[i] == LocalDate.class)
                    value = rs.getDate(snake(fields[i].getName())).toLocalDate();
                else if (types[i] == LocalDateTime.class)
                    value = rs.getTimestamp(snake(fields[i].getName())).toLocalDateTime();
                else if (types[i] == String.class)
                    value = value.toString();
            }
            values[i] = value;
        }
        return type.getDeclaredConstructor(types).newInstance(values);
    }
    private <T> List<T> query(Class<T> type, String sql, Object... args) {
        try (Connection con = DBUtil.getConnection(); PreparedStatement st = con.prepareStatement(sql)) {
            bind(st, args);
            try (ResultSet rs = st.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next())
                    result.add(map(type, rs));
                return result;
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw failure(e);
        }
    }
    private long scalar(String sql, Object... args) {
        try (Connection con = DBUtil.getConnection(); PreparedStatement st = con.prepareStatement(sql)) {
            bind(st, args);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }
    private int execute(String sql, Object... args) {
        try (Connection con = DBUtil.getConnection(); PreparedStatement st = con.prepareStatement(sql)) {
            bind(st, args);
            return st.executeUpdate();
        } catch (SQLException e) {
            throw failure(e);
        }
    }
    private static DataAccessException failure(Exception e) {
        if (e instanceof SQLException sql && sql.getErrorCode() == 1062)
            throw new IllegalArgumentException("이미 사용 중인 아이디 또는 중복된 항목입니다.");
        return new DataAccessException("DB 요청을 처리하지 못했습니다. MySQL 실행 상태와 db.properties 설정, 스키마/덤프 적재 여부를 확인해주세요.", e);
    }
    private long insert(Connection con, String table, Object record) throws SQLException, ReflectiveOperationException {
        if (!TABLES.contains(table))
            throw new IllegalArgumentException("지원하지 않는 테이블입니다.");
        List<String> columns = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        RecordComponent[] fields = record.getClass().getRecordComponents();
        for (int i = 1; i < fields.length; i++) {
            String name = fields[i].getName();
            if (name.equals("createdAt") || name.equals("updatedAt"))
                continue;
            columns.add("`" + snake(name) + "`");
            args.add(fields[i].getAccessor().invoke(record));
        }
        String sql = "INSERT INTO `" + table + "` (" + String.join(",", columns) + ") VALUES ("
                + String.join(",", Collections.nCopies(args.size(), "?")) + ")";
        try (PreparedStatement st = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(st, args.toArray());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (!rs.next())
                    throw new SQLException("생성된 번호가 없습니다.");
                return rs.getLong(1);
            }
        }
    }
    private long insert(String table, Object record) {
        try (Connection con = DBUtil.getConnection()) {
            return insert(con, table, record);
        } catch (SQLException | ReflectiveOperationException e) {
            throw failure(e);
        }
    }
    private void update(String table, String key, Object record) {
        try {
            List<String> columns = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            RecordComponent[] fields = record.getClass().getRecordComponents();
            for (int i = 1; i < fields.length; i++) {
                String name = fields[i].getName();
                if (name.equals("createdAt") || name.equals("updatedAt"))
                    continue;
                columns.add("`" + snake(name) + "`=?");
                args.add(fields[i].getAccessor().invoke(record));
            }
            args.add(fields[0].getAccessor().invoke(record));
            if (execute("UPDATE `" + table + "` SET " + String.join(",", columns) + " WHERE `" + key + "`=?",
                    args.toArray()) == 0)
                throw new IllegalArgumentException("변경할 항목이 없습니다.");
        } catch (ReflectiveOperationException e) {
            throw failure(e);
        }
    }
    private static <T> T first(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
    @Override
    public Map<String, Long> tableCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String table : TABLES)
            result.put(table, scalar("SELECT COUNT(*) FROM `" + table + "`"));
        return result;
    }
    @Override
    public List<SidoDTO> findSidos() {
        return query(SidoDTO.class, "SELECT * FROM sidos ORDER BY sido_code");
    }
    @Override
    public List<GugunDTO> findGuguns(int code) {
        return query(GugunDTO.class, "SELECT * FROM guguns WHERE sido_code=? ORDER BY gugun_code", code);
    }
    @Override
    public List<ContentTypeDTO> findContentTypes() {
        return query(ContentTypeDTO.class, "SELECT * FROM contenttypes ORDER BY content_type_id");
    }
    private record Filter(String sql, List<Object> args) {
    }
    private Filter filter(AttractionSearch search) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (search.sidoCode() != null) {
            sql.append(" AND area_code=?");
            args.add(search.sidoCode());
        }
        if (search.gugunCode() != null) {
            sql.append(" AND si_gun_gu_code=?");
            args.add(search.gugunCode());
        }
        if (search.contentTypeId() != null) {
            sql.append(" AND content_type_id=?");
            args.add(search.contentTypeId());
        }
        if (search.keyword() != null && !search.keyword().isBlank()) {
            sql.append(" AND title LIKE ?");
            args.add("%" + search.keyword() + "%");
        }
        if (search.longitude() != null) {
            sql.append(
                    " AND latitude IS NOT NULL AND longitude IS NOT NULL AND ST_Distance_Sphere(POINT(longitude,latitude),POINT(?,?))<=?");
            args.add(search.longitude());
            args.add(search.latitude());
            args.add(search.radius());
        }
        return new Filter(sql.toString(), args);
    }
    @Override
    public List<AttractionDTO> findAttractions(AttractionSearch search) {
        Filter f = filter(search);
        List<Object> args = new ArrayList<>(f.args());
        String order = " ORDER BY title,no";
        if (search.longitude() != null) {
            order = " ORDER BY ST_Distance_Sphere(POINT(longitude,latitude),POINT(?,?)),no";
            args.add(search.longitude());
            args.add(search.latitude());
        }
        args.add(search.size());
        args.add((search.page() - 1) * search.size());
        return query(AttractionDTO.class, "SELECT * FROM attractions" + f.sql() + order + " LIMIT ? OFFSET ?",
                args.toArray());
    }
    @Override
    public int countAttractions(AttractionSearch s) {
        Filter f = filter(s);
        return Math.toIntExact(scalar("SELECT COUNT(*) FROM attractions" + f.sql(), f.args().toArray()));
    }
    @Override
    public AttractionDTO findAttraction(int no) {
        return first(query(AttractionDTO.class, "SELECT * FROM attractions WHERE no=?", no));
    }
    @Override
    public AttractionDTO findAttractionByContentId(int id) {
        return first(
                query(AttractionDTO.class, "SELECT * FROM attractions WHERE content_id=? ORDER BY no LIMIT 1", id));
    }
    @Override
    public MemberDTO findMember(String loginId) {
        return first(query(MemberDTO.class, "SELECT * FROM members WHERE login_id=?", loginId));
    }
    @Override
    public MemberDTO findMember(long id) {
        return first(query(MemberDTO.class, "SELECT * FROM members WHERE member_id=?", id));
    }
    private Filter memberFilter(SearchCondition c) {
        if (c.hasKeyword())
            return new Filter(" WHERE is_login_enabled=1 AND "
                    + (c.normalizedKey().equals("name") ? "name" : "login_id") + " LIKE ?",
                    List.of("%" + c.getWord().trim() + "%"));
        return new Filter(" WHERE is_login_enabled=1", List.of());
    }
    @Override
    public List<MemberDTO> findMembers(SearchCondition c) {
        Filter f = memberFilter(c);
        List<Object> args = new ArrayList<>(f.args());
        args.add(c.getItemsPerPage());
        args.add(c.getOffset());
        return query(MemberDTO.class, "SELECT * FROM members" + f.sql() + " ORDER BY member_id LIMIT ? OFFSET ?",
                args.toArray());
    }
    @Override
    public int countMembers(SearchCondition c) {
        Filter f = memberFilter(c);
        return Math.toIntExact(scalar("SELECT COUNT(*) FROM members" + f.sql(), f.args().toArray()));
    }
    @Override
    public long insertMember(MemberDTO m) {
        return insert("members", m);
    }
    @Override
    public void updateMember(MemberDTO m) {
        update("members", "member_id", m);
    }
    @Override
    public boolean deactivateMember(String id) {
        return execute(
                "UPDATE members SET is_login_enabled=0,name='탈퇴한 회원',password_hash='!ACCOUNT_DISABLED!',recovery_question=NULL,recovery_answer_hash=NULL WHERE login_id=? AND is_login_enabled=1",
                id) > 0;
    }
    @Override
    public boolean updatePassword(String id, String hash) {
        return execute("UPDATE members SET password_hash=? WHERE login_id=? AND is_login_enabled=1", hash, id) > 0;
    }
    @Override
    public List<PostDTO> findPosts() {
        return query(PostDTO.class, "SELECT * FROM posts ORDER BY created_at DESC,post_id DESC");
    }
    @Override
    public PostDTO findPost(long id) {
        return first(query(PostDTO.class, "SELECT * FROM posts WHERE post_id=?", id));
    }
    @Override
    public long insertPost(PostDTO post) {
        return insert("posts", post);
    }
    @Override
    public void updatePost(PostDTO post) {
        update("posts", "post_id", post);
    }
    @Override
    public boolean deletePost(long id) {
        return execute("DELETE FROM posts WHERE post_id=?", id) > 0;
    }
    @Override
    public List<CommentDTO> findComments() {
        return query(CommentDTO.class, "SELECT * FROM comments ORDER BY created_at,comment_id");
    }
    @Override
    public long insertComment(CommentDTO comment) {
        return insert("comments", comment);
    }
    @Override
    public boolean deleteComment(long id, long actor) {
        return execute("DELETE FROM comments WHERE comment_id=? AND member_id=?", id, actor) > 0;
    }
    @Override
    public Map<Long, String> findAuthorNames() {
        String sql = "SELECT member_id,name FROM members WHERE member_id IN (SELECT member_id FROM posts UNION SELECT member_id FROM comments)";
        try (Connection con = DBUtil.getConnection();
                PreparedStatement st = con.prepareStatement(sql);
                ResultSet rs = st.executeQuery()) {
            Map<Long, String> result = new LinkedHashMap<>();
            while (rs.next())
                result.put(rs.getLong(1), rs.getString(2));
            return result;
        } catch (SQLException e) {
            throw failure(e);
        }
    }
    @Override
    public List<TripPlanDTO> findPlans(long member) {
        return query(TripPlanDTO.class,
                "SELECT * FROM trip_plans WHERE member_id=? ORDER BY created_at DESC,plan_id DESC", member);
    }
    @Override
    public List<TripPlanPlaceDTO> findPlanPlaces(long id) {
        return query(TripPlanPlaceDTO.class, "SELECT * FROM trip_plan_places WHERE plan_id=? ORDER BY visit_order", id);
    }
    @Override
    public long insertPlan(TripPlanDTO plan, List<TripPlanPlaceDTO> places) {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                long id = insert(con, "trip_plans", plan);
                for (TripPlanPlaceDTO p : places)
                    insert(con, "trip_plan_places",
                            new TripPlanPlaceDTO(null, id, p.attractionNo(), p.visitOrder(), p.memo()));
                con.commit();
                return id;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw failure(e);
        }
    }
    @Override
    public boolean deletePlan(long id, long member) {
        return execute("DELETE FROM trip_plans WHERE plan_id=? AND member_id=?", id, member) > 0;
    }
    @Override
    public List<TravelRecordDTO> findRecords(long member) {
        return query(TravelRecordDTO.class,
                "SELECT * FROM travel_records WHERE member_id=? ORDER BY visit_date DESC,record_id DESC", member);
    }
    @Override
    public long insertRecord(TravelRecordDTO r) {
        return insert("travel_records", r);
    }
    @Override
    public boolean deleteRecord(long id, long member) {
        return execute("DELETE FROM travel_records WHERE record_id=? AND member_id=?", id, member) > 0;
    }
}
