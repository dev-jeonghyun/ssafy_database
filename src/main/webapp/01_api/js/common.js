/**
 * 특정 select의 option 내용을 업데이트 하는 함수
 * @param {HTMLSelectElement} select - 옵션을 변경할 대상 요소
 * @param {Array<{key:string, label:string}>} data - option을 구성할 배열
 */

export const updateSelect = (select, data) => {
  const type = select.dataset.type;
  select.replaceChildren(new Option(`${type} 전체`, ""));
  select.disabled = !Array.isArray(data) || data.length === 0;
  if (Array.isArray(data)) {
    data.forEach((item) => {
      select.add(new Option(item.label || item.name, item.key || item.code));
    });
  }
};

/**
 * url에 get 방식의 fetch 요청을 하고 결과를 Promise로 반환한다.
 * @async
 * @param {string} url - fetch 대상 url
 * @param {Object} param - URLSearchParams를 구성할 데이터들
 * @param {boolean} isXml - 서버의 응답이 xml인지 판단 여부
 * @returns {Promise<string|Object>} - isXml==true인 경우 String을 담은 Promise, 아니면 JSON 객체를 담은 Promise
 * @throws {Error} - 네트워크 오류 또는 응답 처리 중 오류 발생 시
 */
export const getFetch = async (url, param, isXml) => {
  try {
    const queryString = new URLSearchParams(param).toString();
    const response = await fetch(url + "?" + queryString);
    if (!response.ok) throw new Error("요청을 처리하지 못했습니다.");
    let result = "";
    if (isXml) {
      result = await response.text();
    } else {
      result = await response.json();
    }
    //console.log("요청 URL: " + url, param, result);
    return result;
  } catch (e) {
    console.log(e);
    throw e;
  }
};
/**
 * select 요소를 전달하면 요소의 label을 반환한다.
 * @param {HTMLSelectElement} select - 대상 Select의 reference
 * @returns {string} - select의 label
 */
export const getLabel = (select) => select.selectedOptions[0].text;

const tourEndpoint = new URL("../../api/tour", import.meta.url);
let configPromise = null;

export async function tourRequest(action, parameters = {}) {
  const url = new URL(tourEndpoint);
  url.searchParams.set("action", action);
  Object.entries(parameters).forEach(([name, value]) => {
    if (value !== undefined && value !== null && value !== "") url.searchParams.set(name, value);
  });
  let response;
  try {
    response = await fetch(url, { headers: { Accept: "application/json" }, credentials: "same-origin" });
  } catch {
    throw new Error("관광정보 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }
  if (!response.headers.get("content-type")?.includes("application/json")) {
    throw new Error("관광정보 요청을 처리하지 못했습니다. 페이지를 새로고침해주세요.");
  }
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || "관광정보를 불러오지 못했습니다.");
  return result;
}

export function getTourConfig() {
  if (!configPromise) configPromise = tourRequest("config");
  return configPromise;
}

export function showTourStatus(message, error = false) {
  const status = document.querySelector("#tour-status");
  if (status) {
    status.textContent = message;
    status.setAttribute("role", error ? "alert" : "status");
  }
}

export function escapeHtml(value = "") {
  return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#39;");
}

export function safeImageUrl(value) {
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) ? url.href : "";
  } catch {
    return "";
  }
}



export async function enjoyTripRequest(action, values = null) {
  const url = new URL("../../api/enjoytrip", import.meta.url);
  url.searchParams.set("action", action);
  const options = { credentials: "same-origin", headers: { Accept: "application/json" } };
  if (values !== null) {
    options.method = "POST";
    options.headers["Content-Type"] = "application/json";
    options.headers["X-Requested-With"] = "XMLHttpRequest";
    options.body = JSON.stringify(values);
  }
  const response = await fetch(url, options);
  if (!response.headers.get("content-type")?.includes("application/json")) throw new Error("DB 요청을 처리하지 못했습니다.");
  const result = await response.json();
  if (!response.ok) { const error = new Error(result.message || "DB 요청에 실패했습니다."); error.status = response.status; throw error; }
  return result;
}
export async function uploadRecordImage(file) {
  const url = new URL("../../api/record-images", import.meta.url);
  const form = new FormData(); form.append("image", file);
  const response = await fetch(url, {method:"POST",credentials:"same-origin",headers:{"X-Requested-With":"XMLHttpRequest",Accept:"application/json"},body:form});
  if (!response.headers.get("content-type")?.includes("application/json")) throw new Error("사진을 업로드하지 못했습니다. 5MB 이하 PNG/JPEG를 선택해주세요.");
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || "사진 업로드에 실패했습니다.");
  return result.imagePath;
}
