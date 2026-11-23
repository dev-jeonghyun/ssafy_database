import { tourRequest, updateSelect, showTourStatus } from "./common.js?v=20261106-map-select-fix";
import { map, updateMap, focusPlace } from "./kakao.js?v=20261106-map-select-fix";

const selectSido = document.querySelector("#selectSido");
const selectGugun = document.querySelector("#selectGugun");
const contentType = document.querySelector("#selectContentType");
const searchButton = document.querySelector("#btn_trip_search");
const aroundButton = document.querySelector("#btn_around_search");
let lastSearch = null;
let searchVersion = 0;
let regionVersion = 0;
let currentBusy = false;
let configured = false;

function filters() {
  return {
    contentTypeId: contentType.value, areaCode: selectSido.value,
    sigunguCode: selectGugun.value
  };
}

function invalidatePaging() {
  lastSearch = null;
  ++searchVersion;
  document.querySelector("#tour-pagination").hidden = true;
  setBusy(false);
}

function setBusy(busy) {
  currentBusy = busy;
  searchButton.disabled = busy || !configured;
  aroundButton.disabled = busy || !configured || !map;
  document.querySelector("#tour-prev").disabled = busy;
  document.querySelector("#tour-next").disabled = busy;
}

export async function initEnjoyTrip(tourConfigured = true) {
  configured = tourConfigured;
  [selectSido, selectGugun, contentType].forEach(select => updateSelect(select));
  contentType.disabled = !configured;
  if (!configured) {
    setBusy(false);
    showTourStatus("DB에 연결하지 못했습니다. MySQL 실행 상태와 DB 설정을 확인해주세요.", true);
    return;
  }
  setBusy(true);
  showTourStatus("지역과 관광 분류를 불러오는 중입니다.");
  const results = await Promise.allSettled([
    tourRequest("regions").then(result => updateSelect(selectSido, result.items)),
    tourRequest("categories").then(result => updateSelect(contentType, result.items))
  ]);
  const failed = results.find(result => result.status === "rejected");
  showTourStatus(failed ? failed.reason.message : "지역과 콘텐츠를 선택한 뒤 관광지를 조회하세요.", Boolean(failed));
  setBusy(false);
}

selectSido.addEventListener("change", async () => {
  invalidatePaging();
  const version = ++regionVersion;
  updateSelect(selectGugun);
  if (!selectSido.value) return;
  try {
    const result = await tourRequest("regions", { areaCode: selectSido.value });
    if (version === regionVersion) updateSelect(selectGugun, result.items);
  } catch (error) {
    if (version === regionVersion) showTourStatus(error.message, true);
  }
});

[selectGugun, contentType].forEach(select => select.addEventListener("change", invalidatePaging));
window.addEventListener("enjoytrip-map-ready", () => setBusy(currentBusy));

function renderResults(result, spots) {
  const list = document.querySelector("#tour-result-list");
  list.replaceChildren();
  spots.forEach(info => {
    const row = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = info.title || "이름 없는 관광지";
    button.addEventListener("click", () => {
      if (!focusPlace(info)) showTourStatus("이 장소의 지도 위치를 표시할 수 없습니다.", true);
    });
    const address = document.createElement("span");
    address.textContent = " · " + (info.addr1 || "주소 정보 없음");
    row.append(button, address);
    list.append(row);
  });
  const pages = Math.max(1, Math.ceil(result.totalCount / result.numOfRows));
  document.querySelector("#tour-pagination").hidden = result.totalCount === 0;
  document.querySelector("#tour-page").textContent = result.pageNo + " / " + pages;
  document.querySelector("#tour-prev").disabled = result.pageNo <= 1;
  document.querySelector("#tour-next").disabled = result.pageNo >= pages;
  showTourStatus(result.totalCount ? "총 " + result.totalCount + "개 중 " + result.pageNo + "페이지를 표시합니다." : "조회된 결과가 없습니다.");
}

async function search(action, parameters) {
  const version = ++searchVersion;
  setBusy(true);
  showTourStatus("관광정보를 조회하는 중입니다.");
  try {
    const result = await tourRequest(action, { ...parameters, numOfRows: 20 });
    if (version !== searchVersion) return;
    const spots = result.items.map(item => ({ ...item, x: item.mapx, y: item.mapy, address: item.addr1, label: item.title }));
    await updateMap(spots);
    if (version !== searchVersion) return;
    lastSearch = { action, parameters: { ...parameters, pageNo: result.pageNo }, result };
    renderResults(result, spots);
  } catch (error) {
    if (version === searchVersion) {
      document.querySelector("#tour-result-list").replaceChildren();
      document.querySelector("#tour-pagination").hidden = true;
      showTourStatus(error.message, true);
    }
  } finally {
    if (version === searchVersion) {
      setBusy(false);
      if (lastSearch) {
        const { result } = lastSearch;
        document.querySelector("#tour-prev").disabled = result.pageNo <= 1;
        document.querySelector("#tour-next").disabled = result.pageNo * result.numOfRows >= result.totalCount;
      }
    }
  }
}

searchButton.addEventListener("click", () => search("search", { ...filters(), pageNo: 1 }));

// 주변 관광지 조회 버튼 클릭 시 이벤트 처리
aroundButton.addEventListener("click", async () => {
  if (!map) {
    showTourStatus("지도를 불러온 뒤 주변 관광지를 조회해주세요.", true);
    return;
  }
  const version = ++searchVersion;
  setBusy(true);
  showTourStatus("주변 위치를 확인하는 중입니다.");
  const center = map.getCenter();
  let position = { mapX: center.getLng(), mapY: center.getLat() };
  if (navigator.geolocation) {
    position = await new Promise(resolve => {
      navigator.geolocation.getCurrentPosition(
        value => resolve({ mapX: value.coords.longitude, mapY: value.coords.latitude }),
        () => resolve(position),
        { timeout: 5000, maximumAge: 60000 }
      );
    });
  }
  if (version !== searchVersion) return;
  map.setCenter(new kakao.maps.LatLng(position.mapY, position.mapX));
  await searchAround(position);
});

// 위치 기반 관광 정보 조회 API (/locationBasedList2) 호출 함수
async function searchAround(position) {
  await search("nearby", { ...filters(), ...position, radius: 2000, pageNo: 1 });
}

document.querySelector("#tour-prev").addEventListener("click", () => {
  if (lastSearch) search(lastSearch.action, { ...lastSearch.parameters, pageNo: lastSearch.result.pageNo - 1 });
});
document.querySelector("#tour-next").addEventListener("click", () => {
  if (lastSearch) search(lastSearch.action, { ...lastSearch.parameters, pageNo: lastSearch.result.pageNo + 1 });
});
