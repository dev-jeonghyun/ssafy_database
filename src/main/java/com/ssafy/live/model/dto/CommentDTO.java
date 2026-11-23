package com.ssafy.live.model.dto;
import java.time.LocalDateTime;
/** comments 테이블의 한 행. */
public record CommentDTO(Long commentId, Long postId, Long memberId, String content, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
