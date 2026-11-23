// F104 - 저장된 여행 계획의 동선을 지도에 표시

import { map, markers } from "./kakao.js?v=20261106-map-select-fix";

// 현재 지도에 표시된 여행 동선
let tripPolyline = null;

// 여행 순서를 표시하는 오버레이 목록
const routeOverlays = [];
const routeMarkers = [];

// 기존 여행 동선 제거
export const clearTripRoute = () => {
  // 기존 선 제거
  if (tripPolyline) {
    tripPolyline.setMap(null);
    tripPolyline = null;
  }

  // 기존 순서 번호 제거
  routeOverlays.forEach((overlay) => {
    overlay.setMap(null);
  });

  routeOverlays.length = 0;

  // 이 동선이 만든 마커만 제거하여 새 관광지 검색 결과는 유지한다.
  routeMarkers.forEach(marker => marker.setMap(null));
  for (let index = markers.length - 1; index >= 0; index--) {
    if (routeMarkers.includes(markers[index])) markers.splice(index, 1);
  }
  routeMarkers.length = 0;
};

// 저장된 여행 계획의 동선을 지도에 표시
export const showTripRoute = (places) => {
  if (!map || !places || places.length === 0) {
    return;
  }

  // 기존 관광지 마커 제거
  markers.forEach((marker) => {
    marker.setMap(null);
  });

  markers.length = 0;

  // 기존 여행 동선 제거
  clearTripRoute();

  const bounds = new kakao.maps.LatLngBounds();
  const linePath = [];

  places.forEach((place, index) => {
    const x = Number(place.mapx);
    const y = Number(place.mapy);

    // 좌표가 올바르지 않은 경우 제외
    if (
      Number.isNaN(x) ||
      Number.isNaN(y) ||
      x < 124.5 ||
      x > 132 ||
      y < 33.0 ||
      y > 38.6
    ) {
      console.warn("여행지 좌표가 유효하지 않습니다:", place);
      return;
    }

    const position = new kakao.maps.LatLng(y, x);

    // 동선 좌표 추가
    linePath.push(position);

    // 지도 범위 추가
    bounds.extend(position);

    // 여행지 마커 생성
    const marker = new kakao.maps.Marker({
      map: map,
      position: position,
    });

    markers.push(marker);
    routeMarkers.push(marker);

    // 마커에 마우스를 올렸을 때 관광지 이름 표시
    const infowindow = new kakao.maps.InfoWindow({
      content: `
        <div style="padding:5px;">
          ${index + 1}. ${place.title}
        </div>
      `,
    });

    kakao.maps.event.addListener(marker, "mouseover", () => {
      infowindow.open(map, marker);
    });

    kakao.maps.event.addListener(marker, "mouseout", () => {
      infowindow.close();
    });

    // 여행 순서를 나타내는 번호
    const numberOverlay = new kakao.maps.CustomOverlay({
      position: position,
      content: `
        <div
          style="
            background:white;
            border:2px solid #333;
            border-radius:50%;
            width:24px;
            height:24px;
            line-height:24px;
            text-align:center;
            font-weight:bold;
          "
        >
          ${index + 1}
        </div>
      `,
      yAnchor: 2.2,
    });

    numberOverlay.setMap(map);

    routeOverlays.push(numberOverlay);
  });

  // 여행지가 2개 이상이면 순서대로 선 연결
  if (linePath.length >= 2) {
    tripPolyline = new kakao.maps.Polyline({
      path: linePath,
      strokeWeight: 4,
      strokeOpacity: 0.8,
      strokeStyle: "solid",
    });

    tripPolyline.setMap(map);
  }

  // 모든 여행지가 지도에 보이도록 범위 설정
  if (linePath.length > 0) {
    map.setBounds(bounds);
  }
};
