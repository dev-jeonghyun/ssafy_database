package com.ssafy.live.model.service;
/** 기존 Servlet/JSP 호출부를 새 DB 서비스에 연결한다. */
public final class BasicMemberService {
    private static final EnjoyTripService SERVICE = new EnjoyTripServiceImpl();
    private BasicMemberService() {
    }
    public static EnjoyTripService getService() {
        return SERVICE;
    }
}
