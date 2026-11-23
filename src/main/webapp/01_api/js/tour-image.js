import { tourRequest } from "./common.js?v=20261106-map-select-fix";

// F112 관광지 사진 조회
export const getTourImages = async (contentId) => {
  try {
    const result = await tourRequest("images", { contentId, numOfRows: 10 });
    return result.items;
  } catch (e) {
    console.warn("관광지 사진을 불러오지 못했습니다.");
    return [];
  }
};
