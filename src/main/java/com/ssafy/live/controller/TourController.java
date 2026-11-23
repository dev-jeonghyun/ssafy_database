package com.ssafy.live.controller;
import java.io.*;
import java.util.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.ssafy.live.model.service.*;
import com.ssafy.live.util.*;

@WebServlet("/api/tour")
public class TourController extends HttpServlet implements ControllerHelper {
    private static final long serialVersionUID = 1L;
    private final EnjoyTripService service = new EnjoyTripServiceImpl();
    private static final Set<String> PARAMETERS = Set.of("pageNo", "numOfRows", "contentTypeId", "contentId",
            "areaCode", "sigunguCode", "keyword", "mapX", "mapY", "radius");
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        String action = Objects.toString(request.getParameter("action"), "");
        try {
            if (action.equals("config")) {
                ApiConfig config = ApiConfig.load(getServletContext());
                boolean configured;
                try (Connection con = DBUtil.getConnection()) {
                    configured = con.isValid(2);
                } catch (SQLException e) {
                    configured = false;
                }
                toJSON(Map.of("kakaoMapKey", config.kakaoMapKey(), "tourConfigured", configured, "dataSource", "mysql"),
                        response);
                return;
            }
            if (!Set.of("regions", "categories", "search", "nearby", "images").contains(action)) {
                response.setStatus(404);
                toJSON(Map.of("message", "요청한 관광 기능을 찾을 수 없습니다."), response);
                return;
            }
            Map<String, String> parameters = new LinkedHashMap<>();
            for (String name : PARAMETERS)
                if (request.getParameter(name) != null)
                    parameters.put(name, request.getParameter(name));
            toJSON(service.queryTour(action, parameters), response);
        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            toJSON(Map.of("message", e.getMessage()), response);
        } catch (DataAccessException e) {
            getServletContext().log("관광 DB 조회 실패", e);
            response.setStatus(503);
            toJSON(Map.of("message", e.getMessage()), response);
        }
    }
}
