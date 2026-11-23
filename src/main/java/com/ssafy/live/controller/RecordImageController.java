package com.ssafy.live.controller;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import com.ssafy.live.util.*;

@WebServlet("/api/record-images")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 6291456, fileSizeThreshold = 0)
public class RecordImageController extends HttpServlet implements ControllerHelper {
    private static final long serialVersionUID = 1L;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            long member = SessionMember.require(request).getMno();
            Path file = ImageStore.file(member, request.getParameter("name"));
            if (!Files.isRegularFile(file)) {
                response.sendError(404);
                return;
            }
            response.setContentType(file.toString().endsWith(".png") ? "image/png" : "image/jpeg");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store");
            Files.copy(file, response.getOutputStream());
        } catch (UnauthenticatedException e) {
            error(response, 401, e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage());
        } catch (DataAccessException e) {
            error(response, 503, e.getMessage());
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                error(response, 403, "페이지를 새로고침한 뒤 다시 요청해주세요.");
                return;
            }
            long member = SessionMember.require(request).getMno();
            Part part = request.getPart("image");
            if (part == null)
                throw new IllegalArgumentException("사진을 선택해주세요.");
            String name;
            try (InputStream in = part.getInputStream()) {
                name = ImageStore.save(member, in.readNBytes(5242881));
            } finally {
                part.delete();
            }
            response.setStatus(201);
            toJSON(Map.of("imagePath", name), response);
        } catch (UnauthenticatedException e) {
            error(response, 401, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            error(response, 400, e.getMessage());
        } catch (DataAccessException e) {
            error(response, 503, e.getMessage());
        }
    }
    private void error(HttpServletResponse response, int status, String message) throws IOException, ServletException {
        response.setStatus(status);
        toJSON(Map.of("message", message), response);
    }
}
