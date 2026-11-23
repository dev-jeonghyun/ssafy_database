package com.ssafy.live.model.dto;
import java.math.BigDecimal;
/** attractions 테이블의 한 행. */
public record AttractionDTO(Integer no, Integer contentId, String title, Integer contentTypeId, Integer areaCode,
        Integer siGunGuCode, String firstImage1, String firstImage2, Integer mapLevel, BigDecimal latitude,
        BigDecimal longitude, String tel, String addr1, String addr2, String homepage, String overview) {
}
