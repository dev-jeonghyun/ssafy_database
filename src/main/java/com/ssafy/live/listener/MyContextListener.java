package com.ssafy.live.listener;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import com.ssafy.live.util.*;
@WebListener
public class MyContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (InputStream in = event.getServletContext().getResourceAsStream("/WEB-INF/.env")) {
            DBUtil.configure(ApiConfig.parse(in));
        } catch (IOException e) {
            throw new IllegalStateException("로컬 환경 설정을 읽지 못했습니다.", e);
        }
    }
}
