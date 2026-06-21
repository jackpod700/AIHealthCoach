<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { useAdminStore } from "../../stores/adminStore";

const adminStore = useAdminStore();
const REFRESH_INTERVAL_MS = 5000;
const lastUpdatedAt = ref(null);
const selectedRequestId = ref(null);
const rejectionReason = ref("");
const reviewForm = reactive(emptyReviewForm());
let refreshTimerId = null;

const users = computed(() => adminStore.dashboard?.users);
const requests = computed(() => adminStore.foodRequestPage?.items || []);
const selectedRequest = computed(() => {
  return requests.value.find((request) => request.id === selectedRequestId.value) || requests.value[0] || null;
});

watch(requests, (nextRequests) => {
  if (!nextRequests.length) {
    selectedRequestId.value = null;
    return;
  }

  if (!nextRequests.some((request) => request.id === selectedRequestId.value)) {
    selectRequest(nextRequests[0]);
  }
});

watch(selectedRequest, (request) => {
  if (request) {
    fillReviewForm(request);
  }
}, { immediate: true });

onMounted(() => {
  loadDashboard();
  adminStore.loadFoodRequests();
  refreshTimerId = window.setInterval(loadDashboard, REFRESH_INTERVAL_MS);
});

onUnmounted(() => {
  if (refreshTimerId) {
    window.clearInterval(refreshTimerId);
  }
});

async function loadDashboard() {
  await adminStore.loadDashboard();

  if (!adminStore.error) {
    lastUpdatedAt.value = new Date();
  }
}

function selectRequest(request) {
  selectedRequestId.value = request.id;
  rejectionReason.value = "";
  fillReviewForm(request);
}

async function approveSelectedRequest() {
  if (!selectedRequest.value) {
    return;
  }

  await adminStore.approveFoodRequest(selectedRequest.value.id, {
    name: reviewForm.name,
    brand: reviewForm.brand || null,
    servingDescription: reviewForm.servingDescription || null,
    servingSize: numberOrNull(reviewForm.servingSize),
    servingUnit: reviewForm.servingUnit || null,
    calories: numberOrZero(reviewForm.calories),
    carbohydrate: numberOrZero(reviewForm.carbohydrate),
    protein: numberOrZero(reviewForm.protein),
    fat: numberOrZero(reviewForm.fat),
    adminNote: reviewForm.adminNote || null,
  });
}

async function rejectSelectedRequest() {
  if (!selectedRequest.value || !rejectionReason.value.trim()) {
    return;
  }

  await adminStore.rejectFoodRequest(selectedRequest.value.id, rejectionReason.value.trim());
  rejectionReason.value = "";
}

function fillReviewForm(request) {
  Object.assign(reviewForm, {
    name: request.name || "",
    brand: request.brand || "",
    servingDescription: request.servingDescription || "",
    servingSize: request.servingSize ?? "",
    servingUnit: request.servingUnit || "",
    calories: request.calories ?? 0,
    carbohydrate: request.carbohydrate ?? 0,
    protein: request.protein ?? 0,
    fat: request.fat ?? 0,
    adminNote: request.adminNote || "",
  });
}

function emptyReviewForm() {
  return {
    name: "",
    brand: "",
    servingDescription: "",
    servingSize: "",
    servingUnit: "",
    calories: 0,
    carbohydrate: 0,
    protein: 0,
    fat: 0,
    adminNote: "",
  };
}

function number(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  return Number(value).toLocaleString("ko-KR");
}

function numberOrNull(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null;
}

function numberOrZero(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatServing(request) {
  if (request.servingDescription) {
    return request.servingDescription;
  }

  return [request.servingSize, request.servingUnit].filter(Boolean).join("") || "-";
}

function lastUpdatedLabel() {
  if (!lastUpdatedAt.value) {
    return "-";
  }
  return lastUpdatedAt.value.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}
</script>

<template>
  <div class="admin-home">
    <AppSidebar />

    <main class="admin-workspace">
      <header class="admin-header">
        <div>
          <p class="section-eyebrow">ADMIN DASHBOARD</p>
          <h1>관리자 대시보드</h1>
          <small class="admin-live-status">
            5초마다 자동 갱신 · 마지막 갱신 {{ lastUpdatedLabel() }}
          </small>
        </div>
        <button type="button" class="admin-refresh-button" :disabled="adminStore.isLoading" @click="loadDashboard">
          <i class="pi pi-refresh"></i>
          <span>{{ adminStore.isLoading ? "갱신 중" : "새로고침" }}</span>
        </button>
      </header>

      <section class="admin-content">
        <div v-if="adminStore.error" class="admin-error">
          {{ adminStore.error }}
        </div>

        <div v-if="!users && adminStore.isLoading" class="admin-loading">
          사용자 현황을 불러오고 있어요.
        </div>

        <section v-if="users" class="admin-user-status">
          <article class="admin-user-card">
            <span>전체 사용자</span>
            <strong>{{ number(users.totalUsers) }}</strong>
            <small>가입된 전체 계정 수</small>
          </article>
          <article class="admin-user-card">
            <span>오늘 가입</span>
            <strong>{{ number(users.todaySignups) }}</strong>
            <small>오늘 생성된 신규 계정</small>
          </article>
          <article class="admin-user-card">
            <span>최근 5분 활성</span>
            <strong>{{ number(users.activeUsers5m) }}</strong>
            <small>최근 5분 내 인증 API 요청 사용자</small>
          </article>
        </section>

        <section class="admin-food-request-layout">
          <section class="admin-food-request-list">
            <div class="admin-section-title">
              <div>
                <p class="section-eyebrow">FOOD REQUESTS</p>
                <h2>음식 등록 요청</h2>
              </div>
              <button
                type="button"
                class="admin-inline-refresh"
                :disabled="adminStore.isLoadingFoodRequests"
                @click="adminStore.loadFoodRequests()"
              >
                <i class="pi pi-refresh"></i>
              </button>
            </div>
            <small class="admin-live-status">대기 중 {{ adminStore.foodRequestPage.totalItems }}건</small>

            <div v-if="adminStore.foodRequestError" class="admin-error">{{ adminStore.foodRequestError }}</div>
            <div v-if="adminStore.foodRequestMessage" class="admin-success">{{ adminStore.foodRequestMessage }}</div>

            <button
              v-for="request in requests"
              :key="request.id"
              type="button"
              class="admin-food-request-item"
              :class="{ selected: selectedRequest?.id === request.id }"
              @click="selectRequest(request)"
            >
              <strong>{{ request.name }}</strong>
              <span>{{ request.brand || "브랜드 없음" }} · {{ formatServing(request) }}</span>
              <small>{{ request.submitterNickname || request.submitterEmail }}</small>
            </button>

            <div v-if="!requests.length && !adminStore.isLoadingFoodRequests" class="admin-empty">
              승인 대기 중인 요청이 없습니다.
            </div>
          </section>

          <aside v-if="selectedRequest" class="admin-food-review-panel">
            <div class="admin-section-title">
              <div>
                <p class="section-eyebrow">REVIEW</p>
                <h2>{{ selectedRequest.name }}</h2>
              </div>
            </div>

            <div class="admin-food-review-grid">
              <label>
                <span>음식명</span>
                <input v-model="reviewForm.name" type="text" />
              </label>
              <label>
                <span>브랜드</span>
                <input v-model="reviewForm.brand" type="text" />
              </label>
              <label>
                <span>기준 설명</span>
                <input v-model="reviewForm.servingDescription" type="text" placeholder="예: 100g, 1인분" />
              </label>
              <label>
                <span>기준 수치</span>
                <input v-model="reviewForm.servingSize" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>기준 단위</span>
                <input v-model="reviewForm.servingUnit" type="text" placeholder="g, ml, 인분" />
              </label>
              <label>
                <span>칼로리</span>
                <input v-model="reviewForm.calories" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>탄수화물</span>
                <input v-model="reviewForm.carbohydrate" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>단백질</span>
                <input v-model="reviewForm.protein" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>지방</span>
                <input v-model="reviewForm.fat" type="number" min="0" step="0.01" />
              </label>
            </div>

            <label class="admin-food-review-note">
              <span>관리자 메모</span>
              <textarea v-model="reviewForm.adminNote" rows="3"></textarea>
            </label>

            <div class="admin-food-review-actions">
              <button type="button" class="admin-primary-button" @click="approveSelectedRequest">
                승인하고 DB에 추가
              </button>
            </div>

            <div class="admin-food-reject-box">
              <label>
                <span>반려 사유</span>
                <textarea v-model="rejectionReason" rows="3"></textarea>
              </label>
              <button type="button" class="admin-secondary-button" :disabled="!rejectionReason.trim()" @click="rejectSelectedRequest">
                반려
              </button>
            </div>
          </aside>
        </section>
      </section>
    </main>
  </div>
</template>
