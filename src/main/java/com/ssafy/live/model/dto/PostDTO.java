package com.ssafy.live.model.dto;
import java.time.LocalDateTime;
/** posts 테이블의 한 행. */
public record PostDTO(Long postId, Long memberId, String postType, String title, String postPasswordHash,
        String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
    @Override
    public String toString() {
        return "PostDTO[id=" + postId + ", title=" + title + "]";
    }
}
