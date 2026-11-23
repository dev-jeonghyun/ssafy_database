import { getTourConfig, showTourStatus } from "./common.js?v=20261106-map-select-fix";
import { loadKakaoMap, initMap } from "./kakao.js?v=20261106-map-select-fix";
import { initEnjoyTrip } from "./enjoytrip.js?v=20261106-map-select-fix";
import { initTravelRecord } from "./travel-record.js?v=20261106-map-select-fix";

async function start() {
  const mapStatus = document.querySelector("#map-status");
  showTourStatus("DB의 지역과 콘텐츠 유형을 준비하는 중입니다.");
  try {
    initTravelRecord();
    const config = await getTourConfig();
    // DB 검색 조건과 지도 SDK를 동시에 준비해 서로의 로딩을 막지 않게 한다.
    const searchReady = initEnjoyTrip(config.tourConfigured).catch(error => {
      showTourStatus(error.message, true);
    });
    const mapReady = (async () => {
      try {
        await loadKakaoMap(config.kakaoMapKey);
        initMap("map");
        mapStatus.textContent = "";
        mapStatus.setAttribute("role", "status");
        window.dispatchEvent(new Event("enjoytrip-map-ready"));
      } catch (error) {
        mapStatus.textContent = error.message;
        mapStatus.setAttribute("role", "alert");
      }
    })();
    await Promise.all([searchReady, mapReady]);
  } catch (error) {
    showTourStatus(error.message, true);
    mapStatus.textContent = "지도 설정을 읽지 못했습니다. 페이지를 새로고침해주세요.";
    mapStatus.setAttribute("role", "alert");
  }
}

start();
