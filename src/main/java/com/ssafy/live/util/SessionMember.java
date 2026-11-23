package com.ssafy.live.util;
import jakarta.servlet.http.*;
import com.ssafy.live.model.dto.Member;
import com.ssafy.live.model.service.BasicMemberService;
public final class SessionMember {
    private SessionMember() {
    }
    public static Member require(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Member saved = session == null ? null : (Member) session.getAttribute("loginUser");
        if (saved == null)
            throw new UnauthenticatedException();
        Member current = BasicMemberService.getService().findByLoginId(saved.getLoginId());
        if (current == null || current.getMno() != saved.getMno()) {
            session.invalidate();
            throw new UnauthenticatedException();
        }
        session.setAttribute("loginUser", current);
        return current;
    }
}
