package com.ssafy.live.model.dto;
import java.time.LocalDateTime;
/** members 테이블의 한 행. */
public record MemberDTO(Long memberId, String loginId, String passwordHash, String name, Boolean isLoginEnabled,
        String role, String recoveryQuestion, String recoveryAnswerHash, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    @Override
    public String toString() {
        return "MemberDTO[id=" + memberId + ", loginId=" + loginId + ", name=" + name + ", role=" + role + "]";
    }
}
