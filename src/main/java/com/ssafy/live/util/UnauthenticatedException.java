package com.ssafy.live.util;
public class UnauthenticatedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public UnauthenticatedException() {
        super("로그인이 필요합니다.");
    }
}
