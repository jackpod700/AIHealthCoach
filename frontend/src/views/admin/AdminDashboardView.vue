<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useAdminStore } from "../../stores/adminStore";

const adminStore = useAdminStore();
const REFRESH_INTERVAL_MS = 5000;
const lastUpdatedAt = ref(null);
const selectedRequestId = ref(null);
const selectedImportSearchMissId = ref(null);
const selectedImportCandidateIds = ref([]);
const activeReviewPanel = ref("requests");
const rejectionReason = ref("");
const importRejectionReason = ref("");
const reviewForm = reactive(emptyReviewForm());
let refreshTimerId = null;

const importStatusOptions = [
  { value: "PENDING_REVIEW", label: "검토 대기" },
  { value: "NO_RESULT", label: "후보 없음" },
  { value: "FAILED", label: "수집 실패" },
  { value: "APPROVED", label: "승인 완료" },
  { value: "REJECTED", label: "거절 완료" },
];

const users = computed(() => adminStore.dashboard?.users);
const requests = computed(() => adminStore.foodRequestPage?.items || []);
const importGroups = computed(() => adminStore.importCandidatePage?.items || []);
const pendingReviewCount = computed(() => {
  return importGroups.value.filter((group) => group.status === "PENDING_REVIEW").length;
});
const selectedImportCandidateCount = computed(() => {
  return selectedImportCandidates.value.length;
});
const selectedRequest = computed(() => {
  return requests.value.find((request) => request.id === selectedRequestId.value) || requests.value[0] || null;
});
const selectedImportGroup = computed(() => {
  return importGroups.value.find((group) => group.searchMissId === selectedImportSearchMissId.value)
    || importGroups.value[0]
    || null;
});
const selectedImportCandidates = computed(() => {
  const candidates = selectedImportGroup.value?.candidates || [];
  const selectedIds = new Set(selectedImportCandidateIds.value);
  return candidates.filter((candidate) => selectedIds.has(candidate.candidateId));
});
const canApproveImportCandidates = computed(() => {
  return selectedImportGroup.value?.status === "PENDING_REVIEW"
    && selectedImportCandidates.value.length > 0
    && selectedImportCandidates.value.every((candidate) => isImportCandidateApprovable(candidate))
    && !adminStore.isLoadingImportCandidates;
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

watch(importGroups, (nextGroups) => {
  if (!nextGroups.length) {
    selectedImportSearchMissId.value = null;
    selectedImportCandidateIds.value = [];
    return;
  }

  if (!nextGroups.some((group) => group.searchMissId === selectedImportSearchMissId.value)) {
    selectImportGroup(nextGroups[0]);
  }
});

watch(selectedImportGroup, (group) => {
  if (!group) {
    selectedImportCandidateIds.value = [];
    return;
  }

  selectedImportCandidateIds.value = defaultImportCandidateIds(group);
}, { immediate: true });

watch(selectedRequest, (request) => {
  if (request) {
    fillReviewForm(request);
  }
}, { immediate: true });

onMounted(() => {
  refreshAdminData();
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

async function refreshAdminData() {
  await loadDashboard();
  await Promise.all([
    adminStore.loadFoodRequests(),
    adminStore.loadImportCandidates(),
  ]);
}

function selectRequest(request) {
  selectedRequestId.value = request.id;
  rejectionReason.value = "";
  fillReviewForm(request);
}

function showReviewPanel(panel) {
  activeReviewPanel.value = panel;
}

function selectImportGroup(group) {
  selectedImportSearchMissId.value = group.searchMissId;
  importRejectionReason.value = "";
  selectedImportCandidateIds.value = defaultImportCandidateIds(group);
}

function toggleImportCandidate(candidate) {
  if (!isImportCandidateApprovable(candidate)) {
    return;
  }

  if (isImportCandidateSelected(candidate)) {
    selectedImportCandidateIds.value = selectedImportCandidateIds.value
      .filter((candidateId) => candidateId !== candidate.candidateId);
    return;
  }

  selectedImportCandidateIds.value = [
    ...selectedImportCandidateIds.value,
    candidate.candidateId,
  ];
}

function changeImportStatus(status) {
  selectedImportSearchMissId.value = null;
  selectedImportCandidateIds.value = [];
  importRejectionReason.value = "";
  adminStore.loadImportCandidates({ status, page: 1, size: adminStore.importCandidatePage.size });
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

async function approveSelectedImportCandidates() {
  if (!canApproveImportCandidates.value) {
    return;
  }

  await adminStore.approveImportCandidates(
    selectedImportGroup.value.searchMissId,
    selectedImportCandidateIds.value
  );
}

async function rejectSelectedRequest() {
  if (!selectedRequest.value || !rejectionReason.value.trim()) {
    return;
  }

  await adminStore.rejectFoodRequest(selectedRequest.value.id, rejectionReason.value.trim());
  rejectionReason.value = "";
}

async function rejectSelectedImportGroup() {
  if (!selectedImportGroup.value || !importRejectionReason.value.trim()) {
    return;
  }

  await adminStore.rejectImportSearchMiss(
    selectedImportGroup.value.searchMissId,
    importRejectionReason.value.trim()
  );
  importRejectionReason.value = "";
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

function formatCandidateServing(candidate) {
  if (candidate.servingDescription) {
    return candidate.servingDescription;
  }

  return [candidate.servingSize, candidate.servingUnit].filter(Boolean).join("") || "-";
}

function importGroupStatusLabel(status) {
  const option = importStatusOptions.find((item) => item.value === status);
  if (option) {
    return option.label;
  }
  return status || "-";
}

function isImportCandidateApprovable(candidate) {
  return candidate?.status === "PENDING" || candidate?.status === "DUPLICATE";
}

function isImportCandidateSelected(candidate) {
  return selectedImportCandidateIds.value.includes(candidate.candidateId);
}

function hasDistinctNormalizedQuery(group) {
  const query = String(group?.query || "").trim().toLowerCase();
  const normalizedQuery = String(group?.normalizedQuery || "").trim().toLowerCase();

  return Boolean(normalizedQuery && normalizedQuery !== query);
}

function defaultImportCandidateIds(group) {
  const candidates = group?.candidates || [];
  const firstCandidate = candidates.find((candidate) => isImportCandidateApprovable(candidate));
  return firstCandidate ? [firstCandidate.candidateId] : [];
}

function selectedImportCandidateNames() {
  return selectedImportCandidates.value
    .map((candidate) => candidate.name)
    .join(", ");
}

function selectedImportCandidateDuplicateLabel() {
  const duplicateCandidates = selectedImportCandidates.value
    .filter((candidate) => candidate.duplicateFoodId);
  if (!duplicateCandidates.length) {
    return "";
  }

  return duplicateCandidates
    .map((candidate) => `#${candidate.duplicateFoodId}`)
    .join(", ");
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
      <header class="admin-header">
        <div>
          <p class="section-eyebrow">ADMIN ONLY</p>
          <h1>관리자 대시보드</h1>
        </div>
        <div class="admin-header-actions">
          <span class="admin-security-badge">
            <i class="pi pi-lock"></i>
            관리자 권한
          </span>
          <button
            type="button"
            class="admin-refresh-button"
            :data-tooltip="`5초마다 자동 갱신 · 마지막 갱신 ${lastUpdatedLabel()}`"
            :disabled="adminStore.isLoading"
            @click="refreshAdminData"
          >
            <i class="pi pi-refresh"></i>
            <span>{{ adminStore.isLoading ? "갱신 중" : "새로고침" }}</span>
          </button>
        </div>
      </header>

      <section class="admin-content">
        <div v-if="adminStore.error" class="admin-error">
          {{ adminStore.error }}
        </div>

        <div v-if="!users && adminStore.isLoading" class="admin-loading">
          사용자 현황을 불러오고 있어요.
        </div>

        <section class="admin-overview-grid">
          <article v-if="users" class="admin-overview-card">
            <span>전체 사용자</span>
            <strong>{{ number(users.totalUsers) }}</strong>
            <small>가입된 전체 계정 수</small>
          </article>
          <article v-if="users" class="admin-overview-card">
            <span>오늘 가입</span>
            <strong>{{ number(users.todaySignups) }}</strong>
            <small>오늘 생성된 신규 계정</small>
          </article>
          <article v-if="users" class="admin-overview-card">
            <span>최근 5분 활성</span>
            <strong>{{ number(users.activeUsers5m) }}</strong>
            <small>최근 5분 내 인증 API 요청 사용자</small>
          </article>
          <button
            type="button"
            class="admin-overview-card admin-overview-card-accent admin-overview-action"
            :class="{ active: activeReviewPanel === 'requests' }"
            @click="showReviewPanel('requests')"
          >
            <span>등록 요청 대기</span>
            <strong>{{ number(adminStore.foodRequestPage.totalItems) }}</strong>
            <small>사용자가 직접 보낸 음식 등록 요청</small>
          </button>
          <button
            type="button"
            class="admin-overview-card admin-overview-card-muted admin-overview-action"
            :class="{ active: activeReviewPanel === 'candidates' }"
            @click="showReviewPanel('candidates')"
          >
            <span>후보 검수 대기</span>
            <strong>{{ number(adminStore.importCandidatePage.totalItems || pendingReviewCount) }}</strong>
            <small>검색 실패 후 수집된 음식 후보</small>
          </button>
        </section>

        <section
          v-if="activeReviewPanel === 'requests'"
          class="admin-food-request-layout admin-review-section"
        >
          <section class="admin-review-list-panel">
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
                aria-label="음식 등록 요청 새로고침"
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
              <i class="pi pi-inbox admin-request-icon"></i>
              <strong>{{ request.name }}</strong>
              <span>{{ request.brand || "브랜드 없음" }} · {{ formatServing(request) }}</span>
              <small>{{ request.submitterNickname || request.submitterEmail }}</small>
            </button>

            <div v-if="!requests.length && !adminStore.isLoadingFoodRequests" class="admin-empty admin-list-empty">
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
              <button type="button" class="admin-primary-button" :disabled="adminStore.isLoadingFoodRequests" @click="approveSelectedRequest">
                <i class="pi pi-check"></i>
                승인하고 DB에 추가
              </button>
            </div>

            <div class="admin-food-reject-box">
              <label>
                <span>반려 사유</span>
                <textarea v-model="rejectionReason" rows="3"></textarea>
              </label>
              <button type="button" class="admin-secondary-button" :disabled="!rejectionReason.trim()" @click="rejectSelectedRequest">
                <i class="pi pi-times"></i>
                반려
              </button>
            </div>
          </aside>
        </section>

        <section
          v-if="activeReviewPanel === 'candidates'"
          class="admin-food-request-layout admin-import-candidate-layout admin-review-section"
        >
          <section class="admin-review-list-panel">
            <div class="admin-section-title">
              <div>
                <p class="section-eyebrow">FATSECRET CANDIDATES</p>
                <h2>음식 후보 검수</h2>
              </div>
              <div class="admin-import-controls">
                <select
                  :value="adminStore.importCandidateStatus"
                  class="admin-status-select"
                  @change="changeImportStatus($event.target.value)"
                >
                  <option v-for="option in importStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
                <button
                  type="button"
                  class="admin-inline-refresh"
                  :disabled="adminStore.isLoadingImportCandidates"
                  @click="adminStore.loadImportCandidates()"
                  aria-label="음식 후보 검수 새로고침"
                >
                  <i class="pi pi-refresh"></i>
                </button>
              </div>
            </div>
            <small class="admin-live-status">
              {{ importGroupStatusLabel(adminStore.importCandidateStatus) }} {{ adminStore.importCandidatePage.totalItems }}건
            </small>

            <div v-if="adminStore.importCandidateError" class="admin-error">{{ adminStore.importCandidateError }}</div>
            <div v-if="adminStore.importCandidateMessage" class="admin-success">{{ adminStore.importCandidateMessage }}</div>

            <button
              v-for="group in importGroups"
              :key="group.searchMissId"
              type="button"
              class="admin-food-request-item admin-candidate-request-item"
              :class="{ selected: selectedImportGroup?.searchMissId === group.searchMissId }"
              @click="selectImportGroup(group)"
            >
              <strong>{{ group.query }}</strong>
              <span>
                {{ importGroupStatusLabel(group.status) }} · 후보 {{ (group.candidates || []).length }}개 · 누적 {{ number(group.missCount) }}회
              </span>
              <small v-if="hasDistinctNormalizedQuery(group)">{{ group.normalizedQuery }}</small>
            </button>

            <div v-if="!importGroups.length && !adminStore.isLoadingImportCandidates" class="admin-empty admin-list-empty">
              선택한 상태의 후보가 없습니다.
            </div>
          </section>

          <aside v-if="selectedImportGroup" class="admin-food-review-panel admin-import-review-panel">
            <div class="admin-section-title">
              <div>
                <p class="section-eyebrow">CANDIDATE REVIEW</p>
                <h2>{{ selectedImportGroup.query }}</h2>
              </div>
              <span class="admin-import-status">{{ importGroupStatusLabel(selectedImportGroup.status) }}</span>
            </div>

            <div v-if="!(selectedImportGroup.candidates || []).length" class="admin-empty">
              이 검색어에는 검수할 후보가 없습니다.
            </div>

            <div v-else class="admin-import-candidate-grid">
              <article
                v-for="candidate in selectedImportGroup.candidates"
                :key="candidate.candidateId"
                class="admin-import-candidate-card"
                :class="{
                  selected: isImportCandidateSelected(candidate),
                  duplicate: candidate.status === 'DUPLICATE',
                }"
                role="button"
                tabindex="0"
                @click="toggleImportCandidate(candidate)"
                @keydown.enter.prevent="toggleImportCandidate(candidate)"
                @keydown.space.prevent="toggleImportCandidate(candidate)"
              >
                <div>
                  <strong>{{ candidate.name }}</strong>
                  <span>{{ candidate.brand || "브랜드 없음" }} · {{ formatCandidateServing(candidate) }}</span>
                </div>
                <a
                  v-if="candidate.sourceUrl"
                  class="admin-candidate-source-link"
                  :href="candidate.sourceUrl"
                  target="_blank"
                  rel="noreferrer"
                  @click.stop
                >
                  원본 보기
                </a>
                <dl>
                  <div>
                    <dt>칼로리</dt>
                    <dd>{{ number(candidate.calories) }} kcal</dd>
                  </div>
                  <div>
                    <dt>탄수</dt>
                    <dd>{{ number(candidate.carbohydrate) }}g</dd>
                  </div>
                  <div>
                    <dt>단백</dt>
                    <dd>{{ number(candidate.protein) }}g</dd>
                  </div>
                  <div>
                    <dt>지방</dt>
                    <dd>{{ number(candidate.fat) }}g</dd>
                  </div>
                </dl>
              </article>
            </div>

            <div v-if="selectedImportCandidates.length" class="admin-import-selected">
              <div>
                <span>선택 후보 {{ selectedImportCandidateCount }}개</span>
                <strong>{{ selectedImportCandidateNames() }}</strong>
                <small v-if="selectedImportCandidateDuplicateLabel()">
                  기존 음식 {{ selectedImportCandidateDuplicateLabel() }}에 연결됩니다.
                </small>
              </div>
            </div>

            <div class="admin-food-review-actions">
              <button
                type="button"
                class="admin-primary-button"
                :disabled="!canApproveImportCandidates"
                @click="approveSelectedImportCandidates"
              >
                <i class="pi pi-check"></i>
                선택 후보 {{ selectedImportCandidateCount }}개 승인
              </button>
            </div>

            <div class="admin-food-reject-box">
              <label>
                <span>검색어 전체 거절 사유</span>
                <textarea v-model="importRejectionReason" rows="3"></textarea>
              </label>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="!importRejectionReason.trim() || selectedImportGroup.status !== 'PENDING_REVIEW'"
                @click="rejectSelectedImportGroup"
              >
                <i class="pi pi-times"></i>
                검색어 후보 전체 거절
              </button>
            </div>
          </aside>
        </section>
      </section>
</template>
