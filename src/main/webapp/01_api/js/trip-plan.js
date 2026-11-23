import { showTripRoute, clearTripRoute } from "./trip-map.js?v=20261106-map-select-fix";
import { escapeHtml, enjoyTripRequest } from "./common.js?v=20261106-map-select-fix";
const tripPlan=[];
let draggedIndex=null;
let selectedSavedPlan=null;
let requestVersion=0;
const clearSelectedTripPlan=()=>{selectedSavedPlan=null;document.querySelector("#selected-trip-plan-detail")?.replaceChildren();clearTripRoute();};
export const addTripPlace = (place) => {
  // 같은 관광지를 중복으로 추가하지 않도록 확인
  const exists = tripPlan.some(
    (item) => item.contentid === place.contentid
  );

  if (exists) {
    alert("이미 여행 계획에 추가된 관광지입니다.");
    return false;
  }

  tripPlan.push(place);

  // 화면의 여행 계획 목록 갱신
  renderTripPlan();
  const list = document.querySelector("#trip-plan-list");
  if (list) {
    list.scrollTop = list.scrollHeight;
    list.closest("section").scrollTop = 0;
  }

  return true;
};

// 현재 여행 계획 반환
export const getTripPlan = () => {
  return tripPlan;
};

// 현재 작성 중인 여행 계획 목록을 화면에 출력
const renderTripPlan = () => {
  const tripPlanList = document.querySelector("#trip-plan-list");

  if (!tripPlanList) {
    return;
  }

  // 기존 목록 초기화
  tripPlanList.innerHTML = "";

  // 여행지가 없는 경우
  if (tripPlan.length === 0) {
    tripPlanList.innerHTML = `
      <p id="empty-trip-plan">
        추가된 여행지가 없습니다.
      </p>
    `;

    return;
  }

  // 여행지 목록 생성
  tripPlan.forEach((place, index) => {
    const item = document.createElement("div");

    item.classList.add("trip-plan-item");

    // Drag & Drop 가능하도록 설정
    item.draggable = true;
    item.dataset.index = index;

    item.innerHTML = `
      <strong>${index + 1}. ${escapeHtml(place.title || place.label)}</strong>
      <span>${escapeHtml(place.addr1 || place.address || "")}</span>
    `;

    // 드래그 시작
    item.addEventListener("dragstart", () => {
      draggedIndex = index;
    });

    // Drop 허용
    item.addEventListener("dragover", (event) => {
      event.preventDefault();
    });

    // 여행지 순서 변경
    item.addEventListener("drop", (event) => {
      event.preventDefault();

      const targetIndex = index;

      if (
        draggedIndex === null ||
        draggedIndex === targetIndex
      ) {
        return;
      }

      // 기존 위치에서 여행지 제거
      const [movedPlace] = tripPlan.splice(
        draggedIndex,
        1
      );

      // 새로운 위치에 삽입
      tripPlan.splice(
        targetIndex,
        0,
        movedPlace
      );

      draggedIndex = null;

      // 변경된 순서로 다시 출력
      renderTripPlan();
    });

    // 드래그 종료
    item.addEventListener("dragend", () => {
      draggedIndex = null;
    });

    tripPlanList.appendChild(item);
  });
};

const renderSelectedTripPlan = (plan) => {
  const detailContainer = document.querySelector(
    "#selected-trip-plan-detail"
  );

  if (!detailContainer) {
    return;
  }

  // 여행지 목록 생성
  const placesHtml = plan.places
    .map(
      (place, index) => `
        <li>
          <strong>${index + 1}. ${escapeHtml(place.title)}</strong>
          ${escapeHtml(place.address || "")}
        </li>
      `
    )
    .join("");

  // 선택한 여행 계획의 상세 정보 표시
  detailContainer.innerHTML = `
    <hr />

    <h3>${escapeHtml(plan.title)}</h3>

    <p>
      <strong>여행 기간:</strong>
      ${escapeHtml(plan.startDate)} ~ ${escapeHtml(plan.endDate)}
    </p>

    <p>
      <strong>여행 경비:</strong>
      ${Number(plan.budget || 0).toLocaleString()}원
    </p>

    <p>
      <strong>상세 계획:</strong>
      ${escapeHtml(plan.detail || "작성된 상세 계획이 없습니다.")}
    </p>

    <h4>여행 동선</h4>

    <ol>
      ${placesHtml}
    </ol>
  `;
};


async function renderSavedTripPlans() {
  const list=document.querySelector("#saved-trip-plan-list"); if(!list)return;
  const version=++requestVersion; list.textContent="여행 계획을 불러오는 중입니다.";
  try {
    const {plans}=await enjoyTripRequest("plans"); if(version!==requestVersion)return;
    list.replaceChildren();
    if(!plans.length){list.textContent="저장된 여행 계획이 없습니다.";clearSelectedTripPlan();return;}
    for(const plan of plans){
      const item=document.createElement("div");item.className="saved-trip-plan-item";
      item.innerHTML=`<strong>${escapeHtml(plan.title)}</strong><span>${escapeHtml(plan.startDate)} ~ ${escapeHtml(plan.endDate)}</span><span>${plan.places.length}개 여행지</span><button type="button" class="btn-show-route">동선 보기</button><button type="button" class="btn-delete-plan btn-delete-record">삭제</button>`;
      item.querySelector(".btn-show-route").addEventListener("click",()=>{selectedSavedPlan=plan.id;showTripRoute(plan.places);renderSelectedTripPlan(plan);});
      item.querySelector(".btn-delete-plan").addEventListener("click",async()=>{
        if(!confirm(`"${plan.title}" 여행 계획을 삭제할까요?`))return;
        try{await enjoyTripRequest("delete-plan",{id:plan.id});if(selectedSavedPlan===plan.id)clearSelectedTripPlan();await renderSavedTripPlans();}catch(error){alert(error.message);}
      });list.append(item);
    }
  }catch(error){if(version===requestVersion){list.textContent=error.status===401?"로그인하면 나의 여행 계획을 볼 수 있습니다.":error.message;clearSelectedTripPlan();}}
}
async function saveTripPlan() {
  const button=document.querySelector("#btn-save-trip-plan"); if(button.disabled)return;button.disabled=true;
  try {
    const plan={title:document.querySelector("#trip-title").value.trim(),startDate:document.querySelector("#trip-start-date").value,endDate:document.querySelector("#trip-end-date").value,budget:document.querySelector("#trip-budget").value||0,detail:document.querySelector("#trip-detail").value.trim(),places:tripPlan.map(p=>({no:p.no,contentid:p.contentid}))};
    await enjoyTripRequest("save-plan",plan); await renderSavedTripPlans();alert("여행 계획을 DB에 저장했습니다.");
  }catch(error){alert(error.message);}finally{button.disabled=false;}
}
window.addEventListener("message",event=>{
 if(event.origin!==location.origin||event.source!==window.parent||event.data?.type!=="enjoytrip-session-change")return;
 ++requestVersion;tripPlan.length=0;renderTripPlan();clearSelectedTripPlan();renderSavedTripPlans();
});
document.addEventListener("DOMContentLoaded",()=>{document.querySelector("#btn-save-trip-plan")?.addEventListener("click",saveTripPlan);renderSavedTripPlans();});
