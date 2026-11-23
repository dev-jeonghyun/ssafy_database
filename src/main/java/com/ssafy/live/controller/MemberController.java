package com.ssafy.live.controller;

import java.io.IOException;
import java.util.Map;

import com.ssafy.live.model.dto.Member;
import com.ssafy.live.model.service.BasicMemberService;
import com.ssafy.live.model.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet implementation class MainController
 */
@WebServlet("/member")
public class MemberController extends HttpServlet implements ControllerHelper {

	private final MemberService mService = BasicMemberService.getService();
	private static final String RESET_LOGIN_ID = "passwordResetLoginId";
	private static final String RESET_MEMBER_NO = "passwordResetMemberNo";
	private static final String RESET_EXPIRES_AT = "passwordResetExpiresAt";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = getActionParameter(request, response);
		switch (action) {
		case "index" -> redirect(request, response, "/index.jsp");
		case "member-regist-form" -> forward(request, response, "/member/member-regist-form.jsp");
		case "member-list-reset" -> memberListReset(request, response);
		case "login-form" -> forward(request, response, "/member/login-form.jsp");
		case "logout" -> logout(request, response);
		case "login-id-check" -> loginIdCheck(request, response);
		case "recovery-form" -> forward(request, response, "/member/password-recovery-form.jsp");
		default -> response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404 처리
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = getActionParameter(request, response);
		switch (action) {
		case "index" -> redirect(request, response, "/index.jsp");
		case "member-regist" -> memberRegist(request, response);
		case "login" -> login(request, response);
		case "recovery-question" -> recoveryQuestion(request, response);
		case "recovery-verify" -> recoveryVerify(request, response);
		case "password-reset" -> passwordReset(request, response);
		default -> response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404 처리
		}
	}

	private void memberRegist(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			String name = request.getParameter("name");
			String loginId = request.getParameter("loginId");
			String pass = request.getParameter("pass");
			checkPasswordConfirmation(pass, request.getParameter("passConfirm"));
			Member member = new Member(name, loginId, pass);
			member.setQuestion(request.getParameter("question"));
			member.setAnswer(request.getParameter("answer"));

			mService.registMember(member);

			String alertMsg = "등록되었습니다. 로그인 후 사용해주세요.";
			HttpSession session = request.getSession();
			session.setAttribute("alertMsg", alertMsg);
			redirect(request, response, "/");
		} catch (Exception e) {
			e.printStackTrace();
			request.setAttribute("error", e.getMessage());
			forward(request, response, "/member/member-regist-form.jsp");
		}
	}

	private void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String loginId = request.getParameter("loginId");
		String pass = request.getParameter("pass");
		Member member = mService.login(loginId, pass);
		if (member != null) {
			HttpSession session = request.getSession();
			request.changeSessionId();
			clearPasswordReset(session);
			session.setAttribute("loginUser", member);
			redirect(request, response, "/");
		} else {
			request.setAttribute("alertMsg", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
			forward(request, response, "/member/login-form.jsp");
		}
	}

	private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		redirect(request, response, "/");
	}

	private void loginIdCheck(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: 03. 전달된 email이 중복되지 않았는지 JSON으로 반환하는 메서드를 작성하세요.
		// 현재 회원 기준에서는 email 대신 loginId의 중복 여부를 확인한다.
		// 반환 형태: Map<String, Boolean> ex: "canUse":true
		String loginId = request.getParameter("loginId");
		boolean canUse = mService.isLoginIdAvailable(loginId);

		Map<String, Boolean> result = Map.of("canUse", canUse);
		toJSON(result, response);
		// END
	}

	private void memberListReset(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		mService.reset();
		HttpSession session = request.getSession();
		session.setAttribute("alertMsg", "회원 목록이 초기화되었습니다.");
		redirect(request, response, "/");
	}

	private void recoveryQuestion(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		clearPasswordReset(request.getSession(false));
		String loginId = request.getParameter("loginId");
		try {
			request.setAttribute("recoveryQuestion", mService.getRecoveryQuestion(loginId));
			request.setAttribute("recoveryLoginId", loginId);
		} catch (IllegalArgumentException e) {
			request.setAttribute("error", e.getMessage());
		}
		forward(request, response, "/member/password-recovery-form.jsp");
	}

	private void recoveryVerify(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		clearPasswordReset(request.getSession(false));
		String loginId = request.getParameter("loginId");
		try {
			String question = mService.getRecoveryQuestion(loginId);
			if (!mService.verifyRecoveryAnswer(loginId, request.getParameter("answer"))) {
				request.setAttribute("recoveryLoginId", loginId);
				request.setAttribute("recoveryQuestion", question);
				throw new IllegalArgumentException("복구 답변이 일치하지 않습니다.");
			}
			HttpSession session = request.getSession();
			session.setAttribute(RESET_LOGIN_ID, loginId);
			session.setAttribute(RESET_MEMBER_NO, mService.findByLoginId(loginId).getMno());
			session.setAttribute(RESET_EXPIRES_AT, System.currentTimeMillis() + 5 * 60 * 1000L);
			request.setAttribute("resetAllowed", true);
		} catch (IllegalArgumentException e) {
			request.setAttribute("error", e.getMessage());
		}
		forward(request, response, "/member/password-recovery-form.jsp");
	}

	private void passwordReset(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		String loginId = session == null ? null : (String) session.getAttribute(RESET_LOGIN_ID);
		Long expiresAt = session == null ? null : (Long) session.getAttribute(RESET_EXPIRES_AT);
		Long memberNo = session == null ? null : (Long) session.getAttribute(RESET_MEMBER_NO);
		Member target = loginId == null ? null : mService.findByLoginId(loginId);
		if (loginId == null || expiresAt == null || expiresAt <= System.currentTimeMillis()
				|| target == null || memberNo == null || target.getMno() != memberNo.longValue()) {
			clearPasswordReset(session);
			request.setAttribute("error", "아이디와 복구 답변을 다시 확인해주세요.");
			forward(request, response, "/member/password-recovery-form.jsp");
			return;
		}
		try {
			String pass = request.getParameter("pass");
			checkPasswordConfirmation(pass, request.getParameter("passConfirm"));
			// 대상 아이디는 답변 검증을 마친 서버 세션에서 가져온다.
			if (!mService.resetPassword(loginId, pass)) {
				clearPasswordReset(session);
				throw new IllegalArgumentException("회원을 찾을 수 없습니다. 복구를 다시 진행해주세요.");
			}
			clearPasswordReset(session);
			Member loginUser = (Member) session.getAttribute("loginUser");
			if (loginUser != null && loginUser.getLoginId().equals(loginId)) {
				session.removeAttribute("loginUser");
			}
			session.setAttribute("alertMsg", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
			redirect(request, response, "/member?action=login-form");
		} catch (IllegalArgumentException e) {
			request.setAttribute("error", e.getMessage());
			request.setAttribute("resetAllowed", session.getAttribute(RESET_LOGIN_ID) != null);
			forward(request, response, "/member/password-recovery-form.jsp");
		}
	}

	private void checkPasswordConfirmation(String pass, String confirmation) {
		if (pass == null || !pass.equals(confirmation)) {
			throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
		}
	}

	private void clearPasswordReset(HttpSession session) {
		if (session != null) {
			session.removeAttribute(RESET_LOGIN_ID);
			session.removeAttribute(RESET_MEMBER_NO);
			session.removeAttribute(RESET_EXPIRES_AT);
		}
	}

	private void template(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

	}
}
