// F105 - 나의 여행 기록

import { geocoder } from "./kakao.js?v=20261106-map-select-fix";
import { enjoyTripRequest, uploadRecordImage } from "./common.js?v=20261106-map-select-fix";
let recordRequestVersion = 0;
let initialized = false;
import { showTravelRecordOnMap, clearTravelRecordOnMap } from "./travel-record-map.js?v=20261106-map-select-fix";

// 현재 미리보기 이미지 URL
let previewImageUrl = null;

// 문자열을 HTML에 안전하게 출력
const escapeHtml = (value = "") => {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
};

// 주소를 카카오 지도 좌표로 변환
const getCoordinatesFromAddress = (address) => {
  return new Promise((resolve, reject) => {
    if (!geocoder) {
      reject(new Error("지도가 아직 초기화되지 않았습니다."));
      return;
    }

    geocoder.addressSearch(address, (result, status) => {
      if (
        status === kakao.maps.services.Status.OK &&
        result.length > 0
      ) {
        resolve({
          mapx: result[0].x,
          mapy: result[0].y,
        });
      } else {
        reject(
          new Error(
            "주소의 위치를 찾을 수 없습니다. 주소를 다시 확인해주세요."
          )
        );
      }
    });
  });
};

// 저장된 여행 기록 목록 출력
const renderSavedTravelRecords = async () => {
  const recordList = document.querySelector(
    "#saved-travel-record-list"
  );

  if (!recordList) {
    return;
  }

  recordList.textContent = "나의 핫플을 불러오는 중입니다.";
  const version = ++recordRequestVersion;
  let savedRecords;
  try { savedRecords = (await enjoyTripRequest("records")).records; }
  catch (error) { if (version === recordRequestVersion) recordList.textContent = error.status === 401 ? "로그인하면 나의 핫플을 볼 수 있습니다." : error.message; return; }
  if (version !== recordRequestVersion) return;

  recordList.innerHTML = "";

  // 저장된 기록이 없는 경우
  if (savedRecords.length === 0) {
    recordList.innerHTML = `
      <p>저장된 여행 기록이 없습니다.</p>
    `;
    return;
  }

  savedRecords.forEach((record, index) => {
    const item = document.createElement("div");

    item.classList.add("saved-travel-record-item");
    item.dataset.index = index;

    const place = escapeHtml(record.place);
    const date = escapeHtml(record.date);
    const type = escapeHtml(record.type);
    const address = escapeHtml(record.address);
    const detail = escapeHtml(record.detail);

    item.innerHTML = `
      <div>
        ${
          record.image
            ? `
              <img
                src="${escapeHtml(record.image)}"
                alt="${place}"
                style="
                  width:150px;
                  height:110px;
                  object-fit:cover;
                "
              />
            `
            : ""
        }
      </div>

      <div>
        <strong>${place}</strong>
      </div>

      <div>
        <strong>다녀온 날짜:</strong>
        ${date}
      </div>

      <div>
        <strong>장소 유형:</strong>
        ${type}
      </div>

      <div>
        <strong>주소:</strong>
        ${address}
      </div>

      <div>
        <strong>상세 설명:</strong>
        ${detail || "작성된 상세 설명이 없습니다."}
      </div>

      <div style="margin-top:8px;">
        <button
          type="button"
          class="btn-show-record-map"
        >
          지도에서 보기
        </button>

        <button
          type="button"
          class="btn-delete-record"
        >
          삭제
        </button>
      </div>

      <hr />
    `;

    // 지도에서 보기
    const mapButton = item.querySelector(
      ".btn-show-record-map"
    );

    mapButton.addEventListener("click", () => {
      showTravelRecordOnMap(record);

      const mapElement =
        document.querySelector("#map");

      if (mapElement) {
        mapElement.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
      }
    });

    // 기록 삭제
    const deleteButton = item.querySelector(
      ".btn-delete-record"
    );

    deleteButton.addEventListener("click", async () => {
      const result = confirm(
        `${record.place} 여행 기록을 삭제하시겠습니까?`
      );

      if (!result) {
        return;
      }

      try { await enjoyTripRequest("delete-record", {id:record.id}); await renderSavedTravelRecords(); }
      catch (error) { alert(error.message); return; }

      alert("여행 기록이 삭제되었습니다.");
    });

    recordList.appendChild(item);
  });
};

// 입력폼 초기화
const resetTravelRecordForm = () => {
  const imageInput =
    document.querySelector("#record-image");

  const imagePreview =
    document.querySelector("#record-image-preview");

  const dateInput =
    document.querySelector("#record-date");

  const typeSelect =
    document.querySelector("#record-type");

  const placeInput =
    document.querySelector("#record-place");

  const addressInput =
    document.querySelector("#record-address");

  const detailInput =
    document.querySelector("#record-detail");

  imageInput.value = "";
  dateInput.value = "";
  typeSelect.value = "";
  placeInput.value = "";
  addressInput.value = "";
  detailInput.value = "";

  imagePreview.innerHTML = "";

  // 미리보기용 Object URL 해제
  if (previewImageUrl) {
    URL.revokeObjectURL(previewImageUrl);
    previewImageUrl = null;
  }
};

// 여행 기록 저장
const saveTravelRecord = async () => {
  const saveButton = document.querySelector("#btn-save-travel-record");
  if (saveButton?.disabled) return;
  const imageInput =
    document.querySelector("#record-image");

  const date =
    document.querySelector("#record-date").value;

  const type =
    document.querySelector("#record-type").value;

  const place =
    document.querySelector("#record-place")
      .value
      .trim();

  const address =
    document.querySelector("#record-address")
      .value
      .trim();

  const detail =
    document.querySelector("#record-detail")
      .value
      .trim();

  const file = imageInput.files[0];

  // 사진 확인
  if (!file) {
    alert("사진을 선택해주세요.");
    return;
  }

  // 이미지 파일 확인
  if (!["image/png", "image/jpeg"].includes(file.type) || file.size > 5 * 1024 * 1024) {
    alert("5MB 이하 PNG 또는 JPEG 사진을 선택해주세요.");
    return;
  }

  // 날짜 확인
  if (!date) {
    alert("다녀온 날짜를 입력해주세요.");
    return;
  }

  // 장소 유형 확인
  if (!type) {
    alert("장소 유형을 선택해주세요.");
    return;
  }

  // 장소명 확인
  if (!place) {
    alert("장소명을 입력해주세요.");
    return;
  }

  // 주소 확인
  if (!address) {
    alert("주소를 입력해주세요.");
    return;
  }

  saveButton.disabled = true;
  try {
    // 주소 → 지도 좌표
    const coordinates =
      geocoder ? await getCoordinatesFromAddress(address) : {mapx:null, mapy:null};

    const imagePath = await uploadRecordImage(file);
    await enjoyTripRequest("save-record", {date,type,place,address,detail,imagePath,mapx:coordinates.mapx,mapy:coordinates.mapy});
    await renderSavedTravelRecords();

    // 입력폼 초기화
    resetTravelRecordForm();

    alert("여행 기록이 저장되었습니다.");
  } catch (error) {
    console.error(error);
    alert(error.message);
  } finally {
    saveButton.disabled = false;
  }
};

// F105 초기화
export const initTravelRecord = () => {
  if (initialized) return;
  initialized = true;
  const imageInput =
    document.querySelector("#record-image");

  const imagePreview =
    document.querySelector("#record-image-preview");

  const saveButton =
    document.querySelector("#btn-save-travel-record");

  if (
    !imageInput ||
    !imagePreview ||
    !saveButton
  ) {
    return;
  }

  // 사진 선택
  imageInput.addEventListener("change", () => {
    const file = imageInput.files[0];

    imagePreview.innerHTML = "";

    // 기존 Object URL 제거
    if (previewImageUrl) {
      URL.revokeObjectURL(previewImageUrl);
      previewImageUrl = null;
    }

    if (!file) {
      return;
    }

    // 이미지 파일 확인
    if (!["image/png", "image/jpeg"].includes(file.type) || file.size > 5 * 1024 * 1024) {
      alert("5MB 이하 PNG 또는 JPEG 사진을 선택해주세요.");

      imageInput.value = "";
      return;
    }

    // 미리보기용 URL 생성
    previewImageUrl =
      URL.createObjectURL(file);

    const image =
      document.createElement("img");

    image.src = previewImageUrl;
    image.alt = "여행 사진 미리보기";

    image.style.width = "200px";
    image.style.height = "150px";
    image.style.objectFit = "cover";
    image.style.marginTop = "10px";

    imagePreview.appendChild(image);
  });

  // 여행 기록 저장
  saveButton.addEventListener(
    "click",
    saveTravelRecord
  );

  // 페이지 진입 시 기존 기록 표시
  renderSavedTravelRecords();
};
window.addEventListener("message", event => {
  if (event.origin!==location.origin || event.source!==window.parent || event.data?.type!=="enjoytrip-session-change") return;
  ++recordRequestVersion; clearTravelRecordOnMap(); resetTravelRecordForm(); renderSavedTravelRecords();
});
