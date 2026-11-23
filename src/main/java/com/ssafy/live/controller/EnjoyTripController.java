package com.ssafy.live.controller;
import java.io.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.live.model.service.*;
import com.ssafy.live.util.*;

@WebServlet("/api/enjoytrip")
public class EnjoyTripController extends HttpServlet implements ControllerHelper {
    private static final long serialVersionUID = 1L;
    private final EnjoyTripService service = new EnjoyTripServiceImpl();
    private static long id(Object value) {
        try {
            long n = Long.parseLong(Objects.toString(value, ""));
            if (n < 0)
                throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("번호가 올바르지 않습니다.");
        }
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        handle(request, response, false);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        handle(request, response, true);
    }
    @SuppressWarnings("unchecked")
    private void handle(HttpServletRequest request, HttpServletResponse response, boolean write)
            throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        try {
            String action = Objects.toString(request.getParameter("action"), "community");
            if (!write && action.equals("community")) {
                toJSON(service.community(), response);
                return;
            }
            if (write && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                error(response, 403, "페이지를 새로고침한 뒤 다시 요청해주세요.");
                return;
            }
            long member = SessionMember.require(request).getMno();
            if (!write) {
                if (action.equals("plans"))
                    toJSON(Map.of("plans", service.plans(member)), response);
                else if (action.equals("records")) {
                    var records = service.records(member);
                    for (var row : records)
                        row.put("image", request.getContextPath() + "/api/record-images?name=" + row.get("imagePath"));
                    toJSON(Map.of("records", records), response);
                } else
                    error(response, 404, "요청한 기능을 찾을 수 없습니다.");
                return;
            }
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                error(response, 415, "JSON 요청이 필요합니다.");
                return;
            }
            byte[] bytes = request.getInputStream().readNBytes(131073);
            if (bytes.length > 131072)
                throw new IllegalArgumentException("요청 내용이 너무 큽니다.");
            Map<String, Object> data;
            try {
                data = new ObjectMapper().readValue(bytes, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("JSON 요청 형식이 올바르지 않습니다.");
            }
            if (data == null)
                throw new IllegalArgumentException("요청 내용이 없습니다.");
            long saved = 0;
            switch (action) {
                case "save-post" -> saved = service.savePost(member, data);
                case "delete-post" ->
                    service.deletePost(member, id(data.get("id")), Objects.toString(data.get("password"), ""));
                case "add-comment" ->
                    service.addComment(member, id(data.get("postId")), Objects.toString(data.get("body"), ""));
                case "delete-comment" -> service.deleteComment(member, id(data.get("id")));
                case "save-plan" -> saved = service.savePlan(member, data);
                case "delete-plan" -> service.deletePlan(member, id(data.get("id")));
                case "save-record" -> saved = service.saveRecord(member, data);
                case "delete-record" -> service.deleteRecord(member, id(data.get("id")));
                default -> {
                    error(response, 404, "요청한 기능을 찾을 수 없습니다.");
                    return;
                }
            }
            toJSON(Map.of("id", Long.toString(saved), "message", "DB에 반영했습니다."), response);
        } catch (UnauthenticatedException e) {
            error(response, 401, e.getMessage());
        } catch (AccessDeniedException e) {
            error(response, 403, e.getMessage());
        } catch (NoSuchElementException e) {
            error(response, 404, e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage());
        } catch (DataAccessException e) {
            getServletContext().log("EnjoyTrip DB 요청 실패", e);
            error(response, 503, e.getMessage());
        }
    }
    private void error(HttpServletResponse response, int status, String message) throws IOException, ServletException {
        response.setStatus(status);
        toJSON(Map.of("message", message), response);
    }
}
