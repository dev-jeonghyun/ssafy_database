package com.ssafy.live.controller;

import java.io.IOException;
import com.ssafy.live.model.dto.Member;
import com.ssafy.live.model.dto.Page;
import com.ssafy.live.model.dto.SearchCondition;
import com.ssafy.live.model.service.BasicMemberService;
import com.ssafy.live.model.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = "/auth")
@SuppressWarnings("serial")
public class AuthController extends HttpServlet implements ControllerHelper {

    private final MemberService mService = BasicMemberService.getService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = getActionParameter(request, response);
        switch (action) {
            case "index" -> redirect(request, response, "/index.jsp");
            case "member-list" -> memberList(request, response);
            case "member-detail" -> memberDetail(request, response);
            case "member-modify-form" -> memberModifyForm(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404 처리
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = getActionParameter(request, response);
        switch (action) {
            case "index" -> redirect(request, response, "/index.jsp");
            case "member-modify" -> memberModify(request, response);
            case "member-delete" -> memberDelete(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404 처리
        }
    }

    private void memberList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String key = request.getParameter("key");
        String word = request.getParameter("word");
        int currentPage = 1;
        String currentPageStr = request.getParameter("currentPage");
        if (currentPageStr != null && !currentPageStr.isBlank()) {
            try {
                currentPage = Integer.parseInt(currentPageStr);
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }

        // TODO: 10. SearchCondition을 구성해서 mService의 search를 조회하세요.
        // 결과는 /member/member-list.jsp에서 확인한다.
        Page<Member> page = mService.search(new SearchCondition(key, word, currentPage));
        request.setAttribute("page", page);
        forward(request, response, "/member/member-list.jsp");
    }

    private void memberDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO: 14. email parameter를 이용하여 회원 정보를 조회하고, request에 저장한다.
        // 현재 회원 기준에서는 email 대신 loginId를 사용한다.
        //  조회 결과는 /member/member-detail.jsp에서 확인한다.
        String loginId = request.getParameter("loginId");
        Member member = mService.findByLoginId(loginId);
        if (member == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("member", member);
        forward(request, response, "/member/member-detail.jsp");
        // END
    }

    private void memberModifyForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // TODO: 17. 회원 정보를 조회하여 정보 수정 화면(/member/member-modify-form.jsp)으로 이동한다.
        String loginId = request.getParameter("loginId");
        Member member = mService.findByLoginId(loginId);
        if (member == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canManageMember(request, response, loginId)) {
            return;
        }
        request.setAttribute("member", member);
        forward(request, response, "/member/member-modify-form.jsp");
        // END
    }

    private void memberModify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String loginId = request.getParameter("loginId");
        String pass = request.getParameter("pass");
        String role = request.getParameter("role");
        // TODO: 18. 회원 정보를 수정 처리한다. 현재 로그인된 사용자의 경우 세션에 저장된 사용자 정보도 변경해준다.
        if (!canManageMember(request, response, loginId)) {
            return;
        }
        Member existingMember = mService.findByLoginId(loginId);
        if (existingMember == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        HttpSession session = request.getSession(false);
        Member loginUser = (Member) session.getAttribute("loginUser");
        Member member = new Member(name, loginId, pass);
        // 회원 번호는 long인 저장된 값을 사용한다. hidden 입력값을 신뢰하지 않는다.
        member.setMno(existingMember.getMno());
        member.setRole("ADMIN".equals(loginUser.getRole()) ? role : existingMember.getRole());
        member.setQuestion(request.getParameter("question"));
        member.setAnswer(request.getParameter("answer"));
        try {
            String confirmation = request.getParameter("passConfirm");
            if (pass != null && !pass.isEmpty() ? !pass.equals(confirmation)
                    : confirmation != null && !confirmation.isEmpty()) {
                throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
            }
            Member updatedMember = mService.memberModify(member, loginUser.getLoginId(),
                    request.getParameter("currentPassword"));
            if (loginUser.getLoginId().equals(loginId)) {
                session.setAttribute("loginUser", updatedMember);
            }
            redirect(request, response, "/auth?action=member-list&currentPage=1");
        } catch (IllegalArgumentException e) {
            request.setAttribute("member", existingMember);
            request.setAttribute("error", e.getMessage());
            forward(request, response, "/member/member-modify-form.jsp");
        }
        // END
    }

    private void memberDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String loginId = request.getParameter("loginId");
        // TODO: 20. memberDelete를 구현해보자.
        // 당사자가 삭제된 경우 - logout 처리 - /로 redirect
        //  다른 회원을 삭제한 경우 - 목록 보기(/auth?action=member-list&currentPage=1)로
        if (!canManageMember(request, response, loginId)) {
            return;
        }
        Member existingMember = mService.findByLoginId(loginId);
        if (existingMember == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        HttpSession session = request.getSession(false);
        Member loginUser = (Member) session.getAttribute("loginUser");
        try {
            if (!mService.memberDelete(loginId, loginUser.getLoginId(), request.getParameter("currentPassword"))) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("member", existingMember);
            request.setAttribute("error", e.getMessage());
            forward(request, response, "/member/member-detail.jsp");
            return;
        }
        if (loginUser.getLoginId().equals(loginId)) {
            session.invalidate();
            redirect(request, response, "/");
            return;
        }
        redirect(request, response, "/auth?action=member-list&currentPage=1");
        // END
    }

    private boolean canManageMember(HttpServletRequest request, HttpServletResponse response, String loginId)
            throws IOException {
        HttpSession session = request.getSession(false);
        Member loginUser = session == null ? null : (Member) session.getAttribute("loginUser");
        if (loginUser == null) {
            redirect(request, response, "/member?action=login-form");
            return false;
        }
        if ("ADMIN".equals(loginUser.getRole()) || loginUser.getLoginId().equals(loginId)) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
