<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import DailyGoalSetupCard from "../../components/chat/DailyGoalSetupCard.vue";
import WeightTrendChart from "../../components/profile/WeightTrendChart.vue";
import { goalOptions } from "../../constants/authOptions";
import { useAuthStore } from "../../stores/authStore";
import { useDailyGoalStore } from "../../stores/dailyGoalStore";
import { useProfileStore } from "../../stores/profileStore";
import { useWeightRecordStore } from "../../stores/weightRecordStore";

const authStore = useAuthStore();
const dailyGoalStore = useDailyGoalStore();
const profileStore = useProfileStore();
const weightRecordStore = useWeightRecordStore();
const router = useRouter();
const isGoalEditorOpen = ref(false);
const todayDateKey = toDateKey(new Date());
const route = useRoute();

const isSetupMode = computed(() => {
  return route.path === "/profile/setup";
});

const profileForm = reactive({
  heightCm: "",
  currentWeightKg: "",
  targetWeightKg: "",
  gender: "FEMALE",
  age: "",
  goalType: "WEIGHT_LOSS",
});

const weightForm = reactive({
  recordDate: todayDateKey,
  weightKg: "",
});

const rangeOptions = [
  { label: "7일", value: "7" },
  { label: "30일", value: "30" },
  { label: "90일", value: "90" },
  { label: "전체", value: "all" },
];

const displayName = computed(() => {
  return (
    authStore.user?.nickname || authStore.user?.email?.split("@")[0] || "사용자"
  );
});

const avatarInitial = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase();
});

const goalLabel = computed(() => {
  return (
    goalOptions.find((goal) => goal.value === profileForm.goalType)?.title ||
    "목표 미설정"
  );
});

const weightDiff = computed(() => {
  const current = Number(profileStore.profile?.currentWeightKg);
  const target = Number(profileStore.profile?.targetWeightKg);

  if (!Number.isFinite(current) || !Number.isFinite(target)) {
    return null;
  }

  return Math.abs(current - target).toFixed(1);
});

const profileUpdatedLabel = computed(() => {
  if (!profileStore.profile?.updatedAt) {
    return "최근 수정일 없음";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(profileStore.profile.updatedAt));
});

const recentWeightRecords = computed(() => {
  return [...weightRecordStore.records]
    .sort((a, b) => b.recordDate.localeCompare(a.recordDate))
    .slice(0, 5);
});

const selectedWeightRecord = computed(
  () => weightRecordStore.recordsByDate[weightForm.recordDate] || null,
);

const canSaveWeightRecord = computed(() => {
  const weightKg = Number(weightForm.weightKg);

  return (
    /^\d{4}-\d{2}-\d{2}$/.test(weightForm.recordDate) &&
    weightForm.recordDate <= todayDateKey &&
    Number.isFinite(weightKg) &&
    weightKg > 0 &&
    weightKg <= 500
  );
});

onMounted(async () => {
  await Promise.all([
    profileStore.loadProfile(),
    weightRecordStore.loadRecords(),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  await profileStore.loadProfile();
});

watch(
  () => profileStore.profile,
  (profile) => {
    if (!profile) {
      return;
    }

    profileForm.heightCm = profile.heightCm ?? "";
    profileForm.currentWeightKg = profile.currentWeightKg ?? "";
    profileForm.targetWeightKg = profile.targetWeightKg ?? "";
    profileForm.gender = profile.gender || "FEMALE";
    profileForm.age = profile.age ?? "";
    profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
  },
  { immediate: true },
);

watch(
  () => selectedWeightRecord.value,
  (record) => {
    if (record) {
      weightForm.weightKg = record.weightKg;
      return;
    }

    if (weightForm.recordDate === todayDateKey) {
      weightForm.weightKg = profileStore.profile?.currentWeightKg ?? "";
    } else {
      weightForm.weightKg = "";
    }
  },
  { immediate: true },
);

function resetForm() {
  const profile = profileStore.profile;

  if (!profile) {
    profileForm.heightCm = "";
    profileForm.currentWeightKg = "";
    profileForm.targetWeightKg = "";
    profileForm.goalType = "WEIGHT_LOSS";
    return;
  }

  profileForm.heightCm = profile.heightCm ?? "";
  profileForm.currentWeightKg = profile.currentWeightKg ?? "";
  profileForm.targetWeightKg = profile.targetWeightKg ?? "";
  profileForm.gender = profile.gender || "FEMALE";
  profileForm.age = profile.age ?? "";
  profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
}

function toNumber(value) {
  if (value === null || value === undefined || String(value).trim() === "") {
    return null;
  }

  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return null;
  }

  return numberValue;
}

function validateProfileForm() {
  const heightCm = toNumber(profileForm.heightCm);
  const currentWeightKg = toNumber(profileForm.currentWeightKg);
  const targetWeightKg = toNumber(profileForm.targetWeightKg);

  if (heightCm === null || heightCm < 50 || heightCm > 300) {
    return "키는 50cm 이상 300cm 이하로 입력해주세요.";
  }

  if (
    currentWeightKg === null ||
    currentWeightKg < 1 ||
    currentWeightKg > 999.99
  ) {
    return "현재 몸무게는 1kg 이상 999.99kg 이하로 입력해주세요.";
  }

  if (
    targetWeightKg === null ||
    targetWeightKg < 1 ||
    targetWeightKg > 999.99
  ) {
    return "목표 몸무게는 1kg 이상 999.99kg 이하로 입력해주세요.";
  }

  if (!profileForm.goalType) {
    return "목표 유형을 선택해주세요.";
  }

  return null;
}

async function saveProfile() {
  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  const validationMessage = validateProfileForm();

  if (validationMessage) {
    profileStore.profileError = validationMessage;
    return;
  }

  await profileStore.updateProfile({
    heightCm: Number(profileForm.heightCm),
    currentWeightKg: Number(profileForm.currentWeightKg),
    targetWeightKg: Number(profileForm.targetWeightKg),
    gender: profileForm.gender,
    age: Number(profileForm.age),
    goalType: profileForm.goalType,
  });

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (!profileStore.profileError && isSetupMode.value) {
    router.replace("/chat");
  }
}

async function openGoalEditor() {
  isGoalEditorOpen.value = true;
  await dailyGoalStore.loadRecommendation(profileForm.goalType);
}

function closeGoalEditor() {
  isGoalEditorOpen.value = false;
}

async function saveGoal(goal) {
  const savedGoal = await dailyGoalStore.saveGoal(goal);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (!savedGoal) {
    return;
  }

  await profileStore.loadProfile();
  profileForm.goalType = savedGoal.goalType;

  closeGoalEditor();
}

async function changeWeightRange(range) {
  await weightRecordStore.loadRecords(range);
}

function editWeightRecord(record) {
  weightForm.recordDate = record.recordDate;
  weightForm.weightKg = record.weightKg;
}

async function saveWeightRecord() {
  if (!canSaveWeightRecord.value) {
    return;
  }

  const savedRecord = await weightRecordStore.saveRecord({
    recordDate: weightForm.recordDate,
    weightKg: Number(weightForm.weightKg),
  });

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (savedRecord) {
    await profileStore.loadProfile();
  }
}

async function deleteWeightRecord(recordDate) {
  const deleted = await weightRecordStore.deleteRecord(recordDate);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (deleted) {
    await profileStore.loadProfile();
  }
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
</script>

<template>
  <main class="profile-home">
    <AppSidebar />

    <section class="profile-workspace">
      <header class="profile-header">
        <div>
          <p class="deco">
            {{ isSetupMode ? "Profile Setup" : "Your Profile" }}
          </p>
          <h1>
            {{ isSetupMode ? "기본 정보 설정" : "프로필" }}
          </h1>
        </div>

        <div class="streak-chip">
          <i></i>
          {{ goalLabel }} 목표 진행 중
        </div>
      </header>

      <div class="profile-body">
        <aside class="profile-summary-card">
          <div class="profile-avatar-large">{{ avatarInitial }}</div>

          <h2>{{ displayName }}</h2>

          <span class="goal-badge"> {{ goalLabel }} 목표 </span>

          <div class="profile-divider"></div>

          <div class="profile-stat-row">
            <div>
              <strong>{{ profileStore.profile?.heightCm ?? "-" }}</strong>
              <small>cm</small>
              <span>키</span>
            </div>

            <div>
              <strong>{{
                profileStore.profile?.currentWeightKg ?? "-"
              }}</strong>
              <small>kg</small>
              <span>현재</span>
            </div>

            <div>
              <strong>{{ profileStore.profile?.targetWeightKg ?? "-" }}</strong>
              <small>kg</small>
              <span>목표</span>
            </div>
          </div>

          <div class="profile-progress-box">
            <div>
              <strong>
                {{
                  weightDiff
                    ? `목표까지 ${weightDiff}kg`
                    : "목표 체중을 설정해주세요"
                }}
              </strong>
              <span>프로필 정보를 입력하면 맞춤 코칭이 시작돼요</span>
            </div>

            <div class="profile-progress-track">
              <i></i>
            </div>

            <p>{{ profileUpdatedLabel }}</p>
          </div>
        </aside>

        <section class="profile-weight-card">
          <header class="profile-weight-header">
            <div>
              <span>Weight Tracking</span>
              <h2>몸무게 기록</h2>
            </div>

            <div class="weight-range-tabs" aria-label="몸무게 기록 기간">
              <button
                v-for="option in rangeOptions"
                :key="option.value"
                type="button"
                :class="{
                  active: weightRecordStore.selectedRange === option.value,
                }"
                @click="changeWeightRange(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </header>

          <div v-if="weightRecordStore.loadError" class="profile-error">
            {{ weightRecordStore.loadError }}
          </div>

          <div class="weight-record-layout">
            <form class="weight-record-form" @submit.prevent="saveWeightRecord">
              <label>
                <span>기록 날짜</span>
                <input
                  v-model="weightForm.recordDate"
                  :max="todayDateKey"
                  type="date"
                />
              </label>

              <label>
                <span>몸무게 (kg)</span>
                <input
                  v-model="weightForm.weightKg"
                  inputmode="decimal"
                  max="500"
                  min="0.01"
                  step="0.1"
                  type="number"
                />
              </label>

              <button
                type="submit"
                :disabled="
                  !canSaveWeightRecord || weightRecordStore.isSavingRecord
                "
              >
                <i class="pi pi-check"></i>
                {{ selectedWeightRecord ? "기록 수정" : "기록 추가" }}
              </button>

              <p v-if="weightRecordStore.saveError">
                {{ weightRecordStore.saveError }}
              </p>
            </form>

            <div class="weight-chart-panel">
              <div
                v-if="weightRecordStore.isLoadingRecords"
                class="profile-loading"
              >
                몸무게 기록을 불러오는 중입니다...
              </div>

              <WeightTrendChart
                v-else
                :records="weightRecordStore.records"
                :target-weight-kg="profileStore.profile?.targetWeightKg"
              />
            </div>
          </div>

          <div class="weight-record-list">
            <article
              v-for="record in recentWeightRecords"
              :key="record.recordDate"
            >
              <button
                type="button"
                class="weight-record-main"
                @click="editWeightRecord(record)"
              >
                <strong>{{ record.weightKg }}kg</strong>
                <span>{{ record.recordDate }}</span>
              </button>
              <button
                type="button"
                class="weight-record-delete"
                :disabled="weightRecordStore.isDeletingRecord"
                :aria-label="`${record.recordDate} 몸무게 기록 삭제`"
                @click="deleteWeightRecord(record.recordDate)"
              >
                <i class="pi pi-trash"></i>
              </button>
            </article>

            <div
              v-if="
                !recentWeightRecords.length &&
                !weightRecordStore.isLoadingRecords
              "
              class="weight-record-empty"
            >
              최근 기록이 없습니다.
            </div>
          </div>
        </section>

        <section class="profile-edit-card">
          <div class="profile-edit-title">
            <h2>
              {{ isSetupMode ? "기본 정보 입력" : "기본 정보 수정" }}
            </h2>
            <p>
              {{
                isSetupMode
                  ? "맞춤형 건강 코칭을 위해 기본 정보와 목표를 입력해주세요."
                  : "정확한 정보일수록 코칭이 더 정밀해져요."
              }}
            </p>
          </div>

          <div v-if="profileStore.profileSuccess" class="profile-success">
            <i class="pi pi-check-circle"></i>
            {{ profileStore.profileSuccess }}
          </div>

          <div v-if="profileStore.profileError" class="profile-error">
            {{ profileStore.profileError }}
          </div>

          <div v-if="profileStore.isLoadingProfile" class="profile-loading">
            프로필 정보를 불러오는 중입니다...
          </div>

          <form v-else class="profile-form-grid" @submit.prevent="saveProfile">
            <label>
              <span>이름</span>
              <input :value="displayName" readonly />
            </label>

            <label>
              <span>키 (cm)</span>
              <input
                v-model="profileForm.heightCm"
                type="number"
                min="50"
                max="300"
                step="0.01"
                inputmode="decimal"
                placeholder="예: 170"
              />
            </label>

            <label>
              <span>현재 몸무게 (kg)</span>
              <input
                v-model="profileForm.currentWeightKg"
                type="number"
                min="1"
                max="999.99"
                step="0.01"
                class="focused-input"
                inputmode="decimal"
                placeholder="예: 70"
              />
            </label>

            <label>
              <span>목표 몸무게 (kg)</span>
              <input
                v-model="profileForm.targetWeightKg"
                type="number"
                min="1"
                max="999.99"
                step="0.01"
                inputmode="decimal"
                placeholder="예: 65"
              />
            </label>
            <label>
              <span>성별</span>
              <div class="segmented-field profile-segmented-field">
                <button
                  type="button"
                  :class="{ active: profileForm.gender === 'FEMALE' }"
                  @click="profileForm.gender = 'FEMALE'"
                >
                  여성
                </button>
                <button
                  type="button"
                  :class="{ active: profileForm.gender === 'MALE' }"
                  @click="profileForm.gender = 'MALE'"
                >
                  남성
                </button>
              </div>
            </label>

            <fieldset>
              <legend>목표 유형</legend>
              <div class="profile-goal-buttons">
                <button
                  v-for="goal in goalOptions"
                  :key="goal.value"
                  type="button"
                  :class="{ active: profileForm.goalType === goal.value }"
                  @click="profileForm.goalType = goal.value"
                >
                  {{ goal.title }}
                </button>
              </div>
            </fieldset>

            <label>
              <span>나이</span>
              <input
                v-model="profileForm.age"
                inputmode="numeric"
                min="1"
                type="number"
              />
            </label>

            <section class="profile-goal-summary">
              <div>
                <span>현재 목표</span>
                <strong>{{ goalLabel }} 목표 진행 중</strong>
              </div>
              <button type="button" @click="openGoalEditor">
                <i class="pi pi-pencil"></i>
                수정
              </button>
            </section>

            <div class="profile-actions">
              <button
                v-if="!isSetupMode"
                class="profile-cancel"
                type="button"
                @click="resetForm"
              >
                취소
              </button>

              <button
                class="profile-save"
                type="submit"
                :disabled="profileStore.isSavingProfile"
              >
                {{
                  profileStore.isSavingProfile
                    ? "저장 중..."
                    : isSetupMode
                      ? "시작하기"
                      : "변경 사항 저장"
                }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="isGoalEditorOpen"
        class="goal-editor-backdrop"
        @click.self="closeGoalEditor"
      >
        <section class="goal-editor-modal">
          <header>
            <div>
              <span>Daily Goal</span>
              <h2>목표 수정</h2>
            </div>
            <button type="button" aria-label="닫기" @click="closeGoalEditor">
              <i class="pi pi-times"></i>
            </button>
          </header>

          <DailyGoalSetupCard
            :initial-goal-type="profileForm.goalType"
            :recommendation="dailyGoalStore.recommendation"
            :is-loading-recommendation="dailyGoalStore.isLoadingRecommendation"
            :is-saving="dailyGoalStore.isSavingGoal"
            :recommendation-error="dailyGoalStore.recommendationError"
            :save-error="dailyGoalStore.saveGoalError"
            title="목표를 수정할까요?"
            description="선택한 목표에 맞춰 하루 섭취량과 운동량을 다시 조정해요."
            submit-label="목표 저장"
            @recommend="dailyGoalStore.loadRecommendation"
            @save="saveGoal"
          />
        </section>
      </div>
    </Teleport>
  </main>
</template>
