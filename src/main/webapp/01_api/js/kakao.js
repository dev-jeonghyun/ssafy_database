// TODO: 01-02. 카카오맵을 초기화 하는 함수를 살펴보세요.

import { getTourConfig, escapeHtml, safeImageUrl } from "./common.js?v=20261106-map-select-fix";
import { getTourImages } from "./tour-image.js?v=20261106-map-select-fix";
import { addTripPlace } from "./trip-plan.js?v=20261106-map-select-fix";

// 카카오 맵 SDK를 동적으로 로드하기 위한 헬퍼 함수
let sdkPromise = null;
export function loadKakaoMap(key) {
  if (sdkPromise) return sdkPromise;
  sdkPromise = (async () => {
    const appKey = key === undefined ? (await getTourConfig()).kakaoMapKey : key;
    if (window.kakao && window.kakao.maps && window.kakao.maps.Map) {
      return;
    }
    if (!appKey) throw new Error("지도 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    await new Promise((resolve, reject) => {
      const script = document.createElement("script");
      const url = new URL("https://dapi.kakao.com/v2/maps/sdk.js");
      url.searchParams.set("appkey", appKey);
      url.searchParams.set("autoload", "false");
      url.searchParams.set("libraries", "services");
      script.src = url.toString();
      script.async = true;
      const timer = setTimeout(() => fail(), 12000);
      function fail() {
        clearTimeout(timer);
        script.remove();
        reject(new Error("지도를 불러오지 못했습니다. 잠시 후 페이지를 새로고침해주세요."));
      }
      script.onerror = fail;
      script.onload = () => {
        if (!window.kakao?.maps?.load) return fail();
        kakao.maps.load(() => { clearTimeout(timer); resolve(); });
      };
      document.head.appendChild(script);
    });
  })().catch(error => { sdkPromise = null; throw error; });
  return sdkPromise;
}

// END

// marker 목록
export let geocoder = null;
export const markers = [];
export let map = null;

// 현재 열려 있는 인포윈도우
let openedInfowindow = null;
let selectedMarkerVersion = 0;
const placeMarkers = new Map();
let mapRenderVersion = 0;

// 지도를 초기화하는 함수
export const initMap = (containerId = "map") => {
  if (map) {
    return; // 이미 초기화 되었다면 종료
  }

  const mapContainer = document.getElementById(containerId); // 지도를 표시할 div

  const mapOption = {
    center: new kakao.maps.LatLng(37.5012748, 127.039625), // 초기 중심: 멀티캠퍼스 역삼 (테헤란로 212)
    level: 3, // 지도의 확대 레벨
  };

  map = new kakao.maps.Map(mapContainer, mapOption);
  geocoder = new kakao.maps.services.Geocoder();

  new ResizeObserver(() => {
    if (!mapContainer.clientWidth || !mapContainer.clientHeight) return;
    const center = map.getCenter();
    map.relayout();
    map.setCenter(center);
  }).observe(mapContainer);
};

export const updateMap = async (infos) => {
  if (!map) return;
  const renderVersion = ++mapRenderVersion;
  ++selectedMarkerVersion;
  placeMarkers.clear();
  markers.forEach((m) => m.setMap(null));
  markers.length = 0; // 마커와 경계 초기화

  // 기존에 열려 있던 인포윈도우가 있으면 닫기
  if (openedInfowindow) {
    openedInfowindow.close();
    openedInfowindow = null;
  }

  const bounds = new kakao.maps.LatLngBounds(); // 지도 경계 초기화

  try {
    // TODO: 01-07. 좌표가 없는 경우 geocoder를 통해 좌표를 갱신해주자.
    const promises = infos
      .filter(
        (info) =>
          info.x == null ||
          info.x == "" ||
          info.y == null ||
          info.y == "",
      )
      .map(
        (info) =>
          new Promise((resolve) => {
            const timeout = setTimeout(resolve, 5000);
            // "geocoder로 좌표 생성"
            geocoder.addressSearch(info.address, function (result, status) {
              clearTimeout(timeout);
              if (status === kakao.maps.services.Status.OK) {
                info.x = result[0].x;
                info.y = result[0].y;
              }
              resolve();
            });
          }),
      );

    // END

    await Promise.all(promises); // 모두 resolve 되면.
    if (renderVersion !== mapRenderVersion) return;

    for (let info of infos) {
      // 잘못된 좌표가 들어오는 경우는 filtering(우리나라만 취급)
      if (
        124.5 <= info.x &&
        info.x <= 132 &&
        33.0 <= info.y &&
        info.y <= 38.6
      ) {
        setOverlay(info, bounds);
      } else {
        console.warn("좌표가 유효하지 않습니다:", info);
      }
    }

    // 지도 경계 설정
    if (markers.length) map.setBounds(bounds);
  } catch (e) {
    console.log(e);
  }
};

// 실제 마커를 등록하고 경계를 확장하는 함수
const setOverlay = (info, bounds) => {
  const coords = new kakao.maps.LatLng(info.y, info.x);
  bounds.extend(coords);

  const marker = new kakao.maps.Marker({
    map: map,
    position: coords,
  });

  markers.push(marker);
  placeMarkers.set(String(info.contentid || info.label), marker);

  // 마우스를 올렸을 때 관광지 이름 표시
  const hoverContent = `<div style="padding:5px;">${escapeHtml(info.label)}</div>`;

  const hoverInfowindow = new kakao.maps.InfoWindow({
    content: hoverContent,
  });

  // 마커에 마우스오버 이벤트 등록
  kakao.maps.event.addListener(marker, "mouseover", function () {
    hoverInfowindow.open(map, marker);
  });

  // 마커에 마우스아웃 이벤트 등록
  kakao.maps.event.addListener(marker, "mouseout", function () {
    hoverInfowindow.close();
  });

  // F112 - 마커 클릭 시 관광지 사진 조회
  kakao.maps.event.addListener(marker, "click", async function () {
    const selection = ++selectedMarkerVersion;
    // 이전에 열려 있던 인포윈도우 닫기
    if (openedInfowindow) {
      openedInfowindow.close();
    }

    const fallbackImage = safeImageUrl(info.firstimage2 || info.firstimage);
    let imageHtml = fallbackImage ? `<img src="${escapeHtml(fallbackImage)}" alt="${escapeHtml(info.label)}" width="200" height="130" />` : `
      <div style="margin-top:8px;">
        등록된 사진이 없습니다.
      </div>
    `;

    // 관광공사 API에서 조회한 관광지만 contentid를 가지고 있음
    if (info.contentid) {
      const images = await getTourImages(info.contentid);
      if (selection !== selectedMarkerVersion) return;

      const image = images.length ? safeImageUrl(images[0].smallimageurl || images[0].originimgurl) : "";
      if (image) {
        imageHtml = `
          <img
            src="${escapeHtml(image)}"
            alt="${escapeHtml(info.label)}"
            style="
              width:200px;
              height:130px;
              object-fit:cover;
              margin-top:8px;
            "
          />
        `;
      }
    }

    // 관광지마다 서로 다른 버튼 id 생성
    const buttonId = `add-trip-${String(info.contentid).replace(/[^a-zA-Z0-9_-]/g, "")}`;

    const clickContent = `
      <div style="
        width:220px;
        padding:10px;
        text-align:center;
      ">
        <strong>${escapeHtml(info.label)}</strong>

        ${imageHtml}

        ${
          info.contentid
            ? `
              <button
                id="${buttonId}"
                style="
                  display:block;
                  margin:8px auto 0;
                  padding:5px 10px;
                  cursor:pointer;
                "
              >
                여행 계획에 추가
              </button>
            `
            : ""
        }
      </div>
    `;

    openedInfowindow = new kakao.maps.InfoWindow({
      content: clickContent,
    });

    openedInfowindow.open(map, marker);

    // F104 - 여행 계획에 관광지 추가
    if (info.contentid) {
      setTimeout(() => {
        const addButton = document.getElementById(buttonId);

        if (addButton) {
          addButton.addEventListener("click", () => {
            const added = addTripPlace(info);

            if (added !== false) {
              alert(`${info.label}을(를) 여행 계획에 추가했습니다.`);
            }
          });
        }
      }, 0);
    }
  });
};

export function focusPlace(info) {
  if (!map || !Number.isFinite(Number(info.x)) || !Number.isFinite(Number(info.y)) || !info.x || !info.y) return false;
  if (Number(info.x) < 124.5 || Number(info.x) > 132 || Number(info.y) < 33 || Number(info.y) > 38.6) return false;
  map.panTo(new kakao.maps.LatLng(Number(info.y), Number(info.x)));
  const marker = placeMarkers.get(String(info.contentid || info.label));
  if (marker) kakao.maps.event.trigger(marker, "click");
  return true;
}
