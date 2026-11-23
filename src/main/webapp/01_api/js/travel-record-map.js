// F105 - 저장된 여행 기록 지도 표시

import { map, markers } from "./kakao.js?v=20261106-map-select-fix";

// 현재 열려 있는 여행 기록 인포윈도우
let recordInfowindow = null;
let recordMarker = null;

export const clearTravelRecordOnMap = () => {
  if (recordInfowindow) recordInfowindow.close();
  recordInfowindow = null;
  if (recordMarker) {
    recordMarker.setMap(null);
    const index = markers.indexOf(recordMarker);
    if (index >= 0) markers.splice(index, 1);
  }
  recordMarker = null;
};

// 문자열을 HTML에 안전하게 출력하기 위한 함수
const escapeHtml = (value = "") => {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
};

// 저장된 여행 기록을 지도에 표시
export const showTravelRecordOnMap = (record) => {
  if (!map || !record) {
    return;
  }

  const x = Number(record.mapx);
  const y = Number(record.mapy);

  // 좌표 확인
  if (
    Number.isNaN(x) ||
    Number.isNaN(y) ||
    x < 124.5 ||
    x > 132 ||
    y < 33.0 ||
    y > 38.6
  ) {
    alert("저장된 위치 정보가 올바르지 않습니다.");
    return;
  }

  // 기존 관광지/여행 계획 마커 제거
  markers.forEach((marker) => {
    marker.setMap(null);
  });

  markers.length = 0;

  // 기존 여행 기록 인포윈도우 제거
  if (recordInfowindow) {
    recordInfowindow.close();
    recordInfowindow = null;
  }

  // 저장된 좌표
  const position = new kakao.maps.LatLng(y, x);

  // 해당 장소로 지도 이동
  map.setCenter(position);
  map.setLevel(3);

  // 여행 기록 마커 생성
  const marker = new kakao.maps.Marker({
    map: map,
    position: position,
  });

  markers.push(marker);
  recordMarker = marker;

  const place = escapeHtml(record.place);
  const date = escapeHtml(record.date);
  const type = escapeHtml(record.type);
  const address = escapeHtml(record.address);
  const detail = escapeHtml(record.detail);

  // 인포윈도우 내용
  const content = `
    <div
      style="
        width:240px;
        padding:12px;
        text-align:center;
        box-sizing:border-box;
      "
    >
      <strong style="font-size:16px;">
        ${place}
      </strong>

      ${
        record.image
          ? `
            <img
              src="${record.image}"
              alt="${place}"
              style="
                display:block;
                width:200px;
                height:150px;
                object-fit:cover;
                margin:10px auto;
              "
            />
          `
          : ""
      }

      <div style="margin-top:5px;">
        <strong>날짜:</strong>
        ${date}
      </div>

      <div style="margin-top:5px;">
        <strong>장소 유형:</strong>
        ${type}
      </div>

      <div style="margin-top:5px;">
        <strong>주소:</strong>
        ${address}
      </div>

      <div style="margin-top:8px;">
        ${detail || "작성된 상세 설명이 없습니다."}
      </div>
    </div>
  `;

  recordInfowindow = new kakao.maps.InfoWindow({
    content: content,
  });

  // 처음부터 상세 내용 표시
  recordInfowindow.open(map, marker);

  // 마커를 다시 클릭해도 상세 내용 표시
  kakao.maps.event.addListener(marker, "click", () => {
    recordInfowindow.open(map, marker);
  });
};
