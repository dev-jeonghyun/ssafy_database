package com.ssafy.live.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.ssafy.live.model.dto.Member;
import com.ssafy.live.util.DataAccessException;
import com.ssafy.live.util.AccessDeniedException;
import com.ssafy.live.model.service.BasicMemberService;
import com.ssafy.live.model.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/member")
@SuppressWarnings("serial")
public class MemberApiController extends HttpServlet implements ControllerHelper {
    private final MemberService service = BasicMemberService.getService();
    private static final String RESET_ID = "passwordResetLoginId";
    private static final String RESET_NO = "passwordResetMemberNo";
    private static final String RESET_EXPIRY = "passwordResetExpiresAt";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            switch (action(request)) {
                case "current" -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("user", publicUser(currentMember(request)));
                    toJSON(result, response);
                }
                case "login-id-check" ->
                    toJSON(Map.of("canUse", service.isLoginIdAvailable(request.getParameter("loginId"))), response);
                case "recovery-question" -> {
                    clearReset(request.getSession(false));
                    String loginId = request.getParameter("loginId");
                    toJSON(Map.of("loginId", loginId, "question", service.getRecoveryQuestion(loginId)), response);
                }
                default -> error(response, 404, "요청한 회원 기능을 찾을 수 없습니다.");
            }
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage());
        } catch (AccessDeniedException e) {
            error(response, 403, e.getMessage());
        } catch (DataAccessException e) {
            getServletContext().log("회원 DB 요청 실패", e);
            error(response, 503, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        // 동일 출처의 JavaScript 요청만 회원 정보를 변경할 수 있다.
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            error(response, 403, "페이지를 새로고침한 뒤 다시 요청해주세요.");
            return;
        }
        try {
            switch (action(request)) {
                case "register" -> register(request, response);
                case "login" -> login(request, response);
                case "logout" -> {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    toJSON(Map.of("message", "로그아웃했습니다."), response);
                }
                case "profile" -> updateProfile(request, response);
                case "delete" -> deleteAccount(request, response);
                case "recovery-verify" -> verifyAnswer(request, response);
                case "password-reset" -> resetPassword(request, response);
                default -> error(response, 404, "요청한 회원 기능을 찾을 수 없습니다.");
            }
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage());
        } catch (AccessDeniedException e) {
            error(response, 403, e.getMessage());
        } catch (DataAccessException e) {
            getServletContext().log("회원 DB 요청 실패", e);
            error(response, 503, e.getMessage());
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String password = confirmedPassword(request);
        Member member = new Member(request.getParameter("name"), request.getParameter("loginId"), password);
        member.setQuestion(request.getParameter("question"));
        member.setAnswer(request.getParameter("answer"));
        service.registMember(member);
        response.setStatus(HttpServletResponse.SC_CREATED);
        toJSON(Map.of("message", "회원가입이 완료되었습니다. 새 계정으로 로그인해주세요."), response);
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Member member = service.login(request.getParameter("loginId"), request.getParameter("password"));
        if (member == null) {
            error(response, 401, "아이디 또는 비밀번호가 일치하지 않습니다.");
            return;
        }
        HttpSession session = request.getSession();
        request.changeSessionId();
        clearReset(session);
        session.setAttribute("loginUser", member);
        toJSON(Map.of("user", publicUser(member)), response);
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Member user = requireMember(request, response);
        if (user == null) {
            return;
        }
        checkOwnId(request, user);
        Member changed = new Member(request.getParameter("name"), user.getLoginId(), confirmedPassword(request));
        changed.setRole(user.getRole());
        changed.setQuestion(request.getParameter("question"));
        changed.setAnswer(request.getParameter("answer"));
        Member saved = service.memberModify(changed, user.getLoginId(), request.getParameter("currentPassword"));
        request.getSession().setAttribute("loginUser", saved);
        toJSON(Map.of("user", publicUser(saved), "message", "회원 정보를 수정했습니다."), response);
    }

    private void deleteAccount(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Member user = requireMember(request, response);
        if (user == null) {
            return;
        }
        checkOwnId(request, user);
        if ("ADMIN".equals(user.getRole())) {
            error(response, 403, "관리자 계정은 이 화면에서 탈퇴할 수 없습니다.");
            return;
        }
        if (!service.memberDelete(user.getLoginId(), user.getLoginId(), request.getParameter("currentPassword"))) {
            error(response, 404, "회원을 찾을 수 없습니다.");
            return;
        }
        request.getSession(false).invalidate();
        toJSON(Map.of("message", "회원탈퇴가 완료되었습니다."), response);
    }

    private void verifyAnswer(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        clearReset(request.getSession(false));
        String loginId = request.getParameter("loginId");
        if (!service.verifyRecoveryAnswer(loginId, request.getParameter("answer"))) {
            throw new IllegalArgumentException("복구 답변이 일치하지 않습니다.");
        }
        Member member = service.findByLoginId(loginId);
        HttpSession session = request.getSession();
        session.setAttribute(RESET_ID, loginId);
        session.setAttribute(RESET_NO, member.getMno());
        session.setAttribute(RESET_EXPIRY, System.currentTimeMillis() + 5 * 60 * 1000L);
        toJSON(Map.of("message", "복구 답변을 확인했습니다."), response);
    }

    private void resetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String loginId = session == null ? null : (String) session.getAttribute(RESET_ID);
        Long memberNo = session == null ? null : (Long) session.getAttribute(RESET_NO);
        Long expires = session == null ? null : (Long) session.getAttribute(RESET_EXPIRY);
        Member member = loginId == null ? null : service.findByLoginId(loginId);
        if (member == null || memberNo == null || member.getMno() != memberNo.longValue() || expires == null
                || expires <= System.currentTimeMillis()) {
            clearReset(session);
            throw new IllegalArgumentException("아이디와 복구 답변을 다시 확인해주세요.");
        }
        if (!service.resetPassword(loginId, confirmedPassword(request))) {
            clearReset(session);
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }
        clearReset(session);
        Member loggedIn = (Member) session.getAttribute("loginUser");
        if (loggedIn != null && loggedIn.getLoginId().equals(loginId)) {
            session.removeAttribute("loginUser");
        }
        toJSON(Map.of("message", "비밀번호를 재설정했습니다. 새 비밀번호로 로그인해주세요."), response);
    }

    private Member currentMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Member saved = session == null ? null : (Member) session.getAttribute("loginUser");
        if (saved == null) {
            return null;
        }
        Member current = service.findByLoginId(saved.getLoginId());
        if (current == null || current.getMno() != saved.getMno()) {
            session.invalidate();
            return null;
        }
        session.setAttribute("loginUser", current);
        return current;
    }

    private Member requireMember(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Member member = currentMember(request);
        if (member == null) {
            error(response, 401, "로그인이 만료되었습니다. 다시 로그인해주세요.");
        }
        return member;
    }

    private void checkOwnId(HttpServletRequest request, Member member) {
        String requested = request.getParameter("loginId");
        if (requested != null && !member.getLoginId().equals(requested)) {
            throw new IllegalArgumentException("아이디는 변경할 수 없습니다.");
        }
    }

    private String confirmedPassword(HttpServletRequest request) {
        String password = Objects.requireNonNullElse(request.getParameter("password"), "");
        String confirmation = Objects.requireNonNullElse(request.getParameter("passwordConfirm"), "");
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }
        return password;
    }

    private Map<String, Object> publicUser(Member member) {
        if (member == null) {
            return null;
        }
        // 비밀번호와 복구 답변은 응답에 포함하지 않는다.
        return Map.of("id", Long.toString(member.getMno()), "loginId", member.getLoginId(), "name", member.getName(),
                "role", Objects.requireNonNullElse(member.getRole(), "USER"), "question",
                Objects.requireNonNullElse(member.getQuestion(), ""));
    }

    private String action(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getParameter("action"), "current");
    }

    private void clearReset(HttpSession session) {
        if (session != null) {
            session.removeAttribute(RESET_ID);
            session.removeAttribute(RESET_NO);
            session.removeAttribute(RESET_EXPIRY);
        }
    }

    private void error(HttpServletResponse response, int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        toJSON(Map.of("message", message), response);
    }
}
