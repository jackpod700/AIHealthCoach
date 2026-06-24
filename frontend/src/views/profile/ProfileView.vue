<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import GoalTypeSelector from "../../components/shared/GoalTypeSelector.vue";
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
const isGoalDetailEditing = ref(false);
const selectedWeightRecordDate = ref("");
const todayDateKey = toDateKey(new Date());

const profileForm = reactive({
  goalType: "WEIGHT_LOSS",
});

const weightForm = reactive({
  recordDate: todayDateKey,
  weightKg: "",
});

const goalDetailForm = reactive({
  goalType: "WEIGHT_LOSS",
  calorieIntakeGoal: 1800,
  exerciseCalorieGoal: 300,
});

const goalRecommendationBase = reactive({
  calorieIntakeGoal: 1800,
  exerciseCalorieGoal: 300,
});

const GOOD_TARGET_MIN_RATIO = 0.9;
const GOOD_TARGET_MAX_RATIO = 1.1;

const GOAL_RANGE_CONFIG = {
  WEIGHT_LOSS: {
    calorie: {
      minOffset: -500,
      maxOffset: 400,
    },
    exercise: {
      minOffset: -150,
      maxOffset: 350,
    },
  },
  MAINTENANCE: {
    calorie: {
      minOffset: -500,
      maxOffset: 500,
    },
    exercise: {
      minOffset: -150,
      maxOffset: 300,
    },
  },
  MUSCLE_GAIN: {
    calorie: {
      minOffset: -400,
      maxOffset: 700,
    },
    exercise: {
      minOffset: -100,
      maxOffset: 400,
    },
  },
};

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

const profileMetaLabel = computed(() => {
  const meta = [];
  const age = profileStore.profile?.age;
  const gender = profileStore.profile?.gender;

  if (age) {
    meta.push(`${age}세`);
  }

  if (gender === "FEMALE") {
    meta.push("여성");
  } else if (gender === "MALE") {
    meta.push("남성");
  }

  return meta.join(" · ");
});

const savedGoalLabel = computed(() => {
  return (
    goalOptions.find((goal) => goal.value === profileStore.profile?.goalType)
      ?.title || "목표 미설정"
  );
});

const selectedWeightRecord = computed(
  () => weightRecordStore.recordsByDate[selectedWeightRecordDate.value] || null,
);

const selectedWeightTitle = computed(() => {
  if (selectedWeightRecord.value) {
    return "선택한 체중 기록";
  }

  return weightForm.recordDate === todayDateKey ? "오늘 기록" : "새 기록";
});

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

const calorieGoal = computed(() => {
  return (
    dailyGoalStore.progress?.progress?.calorieIntake?.goal ??
    dailyGoalStore.currentGoal?.calorieIntakeGoal ??
    goalDetailForm.calorieIntakeGoal
  );
});

const displayCalorieGoal = computed(() =>
  roundToStep(goalDetailForm.calorieIntakeGoal, 50),
);

const displayExerciseGoal = computed(() =>
  roundToStep(goalDetailForm.exerciseCalorieGoal, 50),
);

const exerciseGoal = computed(() => {
  return (
    dailyGoalStore.progress?.progress?.exerciseCalories?.goal ??
    dailyGoalStore.currentGoal?.exerciseCalorieGoal ??
    goalDetailForm.exerciseCalorieGoal
  );
});

const overviewCalorieGoal = computed(() => {
  return isGoalDetailEditing.value ? displayCalorieGoal.value : calorieGoal.value;
});

const overviewExerciseGoal = computed(() => {
  return isGoalDetailEditing.value ? displayExerciseGoal.value : exerciseGoal.value;
});

const activeGoalRangeConfig = computed(() => {
  return (
    GOAL_RANGE_CONFIG[goalDetailForm.goalType] || GOAL_RANGE_CONFIG.WEIGHT_LOSS
  );
});

const calorieRange = computed(() => {
  const base = goalRecommendationBase.calorieIntakeGoal;
  const config = activeGoalRangeConfig.value.calorie;

  return {
    min: Math.max(1000, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
  };
});

const exerciseRange = computed(() => {
  const base = goalRecommendationBase.exerciseCalorieGoal;
  const config = activeGoalRangeConfig.value.exercise;

  return {
    min: Math.max(0, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
  };
});

const calorieSliderStyle = computed(() =>
  goalSliderStyle(
    goalDetailForm.calorieIntakeGoal,
    calorieRange.value.min,
    calorieRange.value.max,
    calorieGoalStatus.value,
  ),
);

const exerciseSliderStyle = computed(() =>
  goalSliderStyle(
    goalDetailForm.exerciseCalorieGoal,
    exerciseRange.value.min,
    exerciseRange.value.max,
    exerciseGoalStatus.value,
  ),
);

const activeGoalRecommendation = computed(() => {
  return dailyGoalStore.recommendations?.[goalDetailForm.goalType] ?? null;
});

const calorieGoalStatus = computed(() =>
  goalTargetStatus(
    displayCalorieGoal.value,
    activeGoalRecommendation.value?.calorieIntakeGoal,
  ),
);

const exerciseGoalStatus = computed(() =>
  goalTargetStatus(
    displayExerciseGoal.value,
    activeGoalRecommendation.value?.exerciseCalorieGoal,
  ),
);

const overallGoalStatus = computed(() => {
  if (
    calorieGoalStatus.value === "unavailable" ||
    exerciseGoalStatus.value === "unavailable"
  ) {
    return "unavailable";
  }

  if (calorieGoalStatus.value === "good" && exerciseGoalStatus.value === "good") {
    return "good";
  }

  return "warning";
});

const calorieGoalGuideMessage = computed(() => {
  return goalGuideMessage(calorieGoalStatus.value, "섭취");
});

const exerciseGoalGuideMessage = computed(() => {
  return goalGuideMessage(exerciseGoalStatus.value, "운동");
});

const calorieGoalStatusLabel = computed(() => goalStatusLabel(calorieGoalStatus.value));
const exerciseGoalStatusLabel = computed(() => goalStatusLabel(exerciseGoalStatus.value));

const goalFooterMessage = computed(() => {
  if (dailyGoalStore.recommendationError) {
    return "추천값을 불러오지 못했어요. 저장은 가능하지만 추천 범위 판단은 잠시 사용할 수 없어요.";
  }

  if (overallGoalStatus.value === "unavailable") {
    return "추천값을 불러오는 중입니다. 저장은 계속 가능해요.";
  }

  if (overallGoalStatus.value === "good") {
    return "추천 범위 안의 좋은 목표예요. 저장하면 오늘 목표에 반영돼요.";
  }

  return "저장은 가능하지만 추천 범위를 벗어난 목표가 있어요.";
});

onMounted(async () => {
  await Promise.all([
    profileStore.loadProfile(),
    weightRecordStore.loadRecords(),
    dailyGoalStore.loadProgress(todayDateKey),
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

    profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
    goalDetailForm.goalType = profile.goalType || "WEIGHT_LOSS";
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

async function startGoalDetailEdit() {
  isGoalDetailEditing.value = true;
  goalDetailForm.goalType = profileForm.goalType;
  goalDetailForm.calorieIntakeGoal = Number(calorieGoal.value) || 1800;
  goalDetailForm.exerciseCalorieGoal = Number(exerciseGoal.value) || 300;
  goalRecommendationBase.calorieIntakeGoal = goalDetailForm.calorieIntakeGoal;
  goalRecommendationBase.exerciseCalorieGoal = goalDetailForm.exerciseCalorieGoal;

  const recommendation = await dailyGoalStore.loadRecommendations(goalDetailForm.goalType, {
    force: true,
  });
  if (recommendation) {
    applyGoalRecommendation(goalDetailForm.goalType, recommendation);
  }
}

function cancelGoalDetailEdit() {
  isGoalDetailEditing.value = false;
  goalDetailForm.goalType = profileStore.profile?.goalType || "WEIGHT_LOSS";
  profileForm.goalType = profileStore.profile?.goalType || "WEIGHT_LOSS";
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
  await dailyGoalStore.loadProgress(todayDateKey);
  profileForm.goalType = savedGoal.goalType;
  goalDetailForm.goalType = savedGoal.goalType;

  isGoalDetailEditing.value = false;
}

function selectGoalDetail(goalType) {
  if (!isGoalDetailEditing.value || goalType === goalDetailForm.goalType) {
    return;
  }

  const recommendation = dailyGoalStore.selectRecommendation(goalType);
  if (recommendation) {
    applyGoalRecommendation(goalType, recommendation);
    return;
  }

  goalDetailForm.goalType = goalType;
  profileForm.goalType = goalType;
}

async function saveGoalDetail() {
  await saveGoal({
    goalType: goalDetailForm.goalType,
    calorieIntakeGoal: displayCalorieGoal.value,
    exerciseCalorieGoal: displayExerciseGoal.value,
  });
}

function formatNumber(value) {
  return Math.round(Number(value) || 0).toLocaleString("ko-KR");
}

function roundToStep(value, step) {
  return Math.round(Number(value || 0) / step) * step;
}

function applyGoalRecommendation(goalType, recommendation) {
  const calorieIntakeGoal = Number(recommendation.calorieIntakeGoal);
  const exerciseCalorieGoal = Number(recommendation.exerciseCalorieGoal);

  goalRecommendationBase.calorieIntakeGoal = calorieIntakeGoal;
  goalRecommendationBase.exerciseCalorieGoal = exerciseCalorieGoal;
  goalDetailForm.goalType = goalType;
  profileForm.goalType = goalType;
  goalDetailForm.calorieIntakeGoal = calorieIntakeGoal;
  goalDetailForm.exerciseCalorieGoal = exerciseCalorieGoal;
}

function goalSliderStyle(value, min, max, status) {
  const percent = goalSliderPercent(value, min, max);
  const color = goalStatusColor(status);

  return {
    "--goal-slider-percent": `${percent}%`,
    "--goal-slider-color": color,
  };
}

function goalSliderPercent(value, min, max) {
  return Math.min(
    Math.max(((Number(value) - min) / Math.max(max - min, 1)) * 100, 0),
    100,
  );
}

function goalTargetStatus(value, recommended) {
  const target = Number(recommended);

  if (!Number.isFinite(target) || target <= 0) {
    return "unavailable";
  }

  if (value < target * GOOD_TARGET_MIN_RATIO) {
    return "low";
  }

  if (value > target * GOOD_TARGET_MAX_RATIO) {
    return "high";
  }

  return "good";
}

function goalStatusLabel(status) {
  if (status === "low") {
    return "너무 낮음";
  }

  if (status === "high") {
    return "너무 높음";
  }

  if (status === "unavailable") {
    return "추천 확인 중";
  }

  return "좋은 목표";
}

function goalGuideMessage(status, metricLabel) {
  if (status === "low") {
    return `목표 ${metricLabel}가 추천보다 낮아요. 조금 올려보면 더 균형 잡힌 목표가 돼요.`;
  }

  if (status === "high") {
    return `목표 ${metricLabel}가 추천보다 높아요. 부담이 크지 않도록 낮추는 것도 좋아요.`;
  }

  if (status === "unavailable") {
    return "추천값을 기준으로 목표 범위를 확인하는 중이에요.";
  }

  return `목표 ${metricLabel}가 추천 범위 안에 있어요. 좋은 목표입니다.`;
}

function goalStatusColor(status) {
  if (status === "low") {
    return "#d89b2f";
  }

  if (status === "high") {
    return "#d6453f";
  }

  if (status === "unavailable") {
    return "#8a908a";
  }

  return "#2f8a55";
}

async function changeWeightRange(range) {
  await weightRecordStore.loadRecords(range);
}

function selectWeightRecord(record) {
  selectedWeightRecordDate.value = record.recordDate;
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
    selectedWeightRecordDate.value = savedRecord.recordDate;
    weightForm.recordDate = savedRecord.recordDate;
    weightForm.weightKg = savedRecord.weightKg;
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
    selectedWeightRecordDate.value = "";
    weightForm.recordDate = todayDateKey;
    weightForm.weightKg = profileStore.profile?.currentWeightKg ?? "";
  }
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function scrollToProfileSection(sectionId) {
  document.getElementById(sectionId)?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
}
</script>

<template>
  <div class="profile-page">
      <nav class="profile-tabbar" aria-label="프로필 섹션">
        <button type="button" @click="scrollToProfileSection('sec-myinfo')">
          내 정보
        </button>
        <button type="button" @click="scrollToProfileSection('sec-goal')">
          목표
        </button>
        <button type="button" @click="scrollToProfileSection('sec-weight')">
          체중
        </button>
      </nav>

      <div class="profile-body">
        <aside id="sec-myinfo" class="profile-summary-card">
          <div class="profile-section-title">내 정보</div>

          <div class="profile-identity-row">
            <div class="profile-avatar-large">{{ avatarInitial }}</div>

            <div class="profile-identity-copy">
              <div class="profile-name-line">
                <h2>{{ displayName }}</h2>
                <span v-if="profileMetaLabel">{{ profileMetaLabel }}</span>
              </div>

              <span class="profile-email">{{ authStore.user?.email }}</span>
            </div>

            <div class="profile-status-stack">
              <span class="goal-badge"> {{ savedGoalLabel }} 목표 </span>
              <span v-if="authStore.isAuthenticated" class="auth-badge">
                로그인 계정
              </span>
            </div>
          </div>

          <div class="profile-divider"></div>

          <div class="profile-stat-row">
            <div>
              <span>키</span>
              <strong>{{ profileStore.profile?.heightCm ?? "-" }}</strong>
              <small>cm</small>
            </div>

            <div>
              <span>현재 체중</span>
              <strong>{{
                profileStore.profile?.currentWeightKg ?? "-"
              }}</strong>
              <small>kg</small>
            </div>

            <div>
              <span>목표 체중</span>
              <strong>{{ profileStore.profile?.targetWeightKg ?? "-" }}</strong>
              <small>kg</small>
            </div>
          </div>

        </aside>

        <section id="sec-weight" class="profile-weight-card">
          <header class="profile-weight-header">
            <div>
              <span>Weight Tracking</span>
              <h2>체중 그래프</h2>
            </div>
          </header>

          <div v-if="weightRecordStore.loadError" class="profile-error">
            {{ weightRecordStore.loadError }}
          </div>

          <div
            class="weight-record-layout"
            :class="{ 'has-selected-record': selectedWeightRecord }"
          >
            <Transition name="weight-record-editor">
              <aside
                v-if="selectedWeightRecord"
                class="weight-record-editor"
                aria-live="polite"
              >
                <div class="weight-record-editor-head">
                  <span>{{ selectedWeightTitle }}</span>
                </div>

                <form class="weight-record-form" @submit.prevent="saveWeightRecord">
                  <label>
                    <span>기록 날짜</span>
                    <strong class="weight-record-date-display">
                      {{ weightForm.recordDate }}
                    </strong>
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

                  <div class="weight-record-actions">
                    <button
                      type="submit"
                      :disabled="
                        !canSaveWeightRecord || weightRecordStore.isSavingRecord
                      "
                    >
                      <i class="pi pi-check"></i>
                      수정
                    </button>
                    <button
                      type="button"
                      class="danger"
                      :disabled="weightRecordStore.isDeletingRecord"
                      @click="deleteWeightRecord(selectedWeightRecord.recordDate)"
                    >
                      <i class="pi pi-trash"></i>
                      삭제
                    </button>
                  </div>

                  <p v-if="weightRecordStore.saveError">
                    {{ weightRecordStore.saveError }}
                  </p>
                  <p v-if="weightRecordStore.deleteError">
                    {{ weightRecordStore.deleteError }}
                  </p>
                </form>
              </aside>
            </Transition>

            <div class="weight-chart-panel">
              <div class="weight-range-tabs" aria-label="몸무게 기록 기간">
                <button
                  v-for="option in rangeOptions"
                  :key="option.value"
                  type="button"
                  :aria-pressed="weightRecordStore.selectedRange === option.value"
                  :class="{
                    active: weightRecordStore.selectedRange === option.value,
                  }"
                  @click="changeWeightRange(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>

              <WeightTrendChart
                :records="weightRecordStore.records"
                :target-weight-kg="profileStore.profile?.targetWeightKg"
                :selected-record-date="selectedWeightRecordDate"
                @select-record="selectWeightRecord"
              />

              <div
                v-if="weightRecordStore.isLoadingRecords"
                class="weight-chart-loading-text"
              >
                몸무게 기록을 불러오는 중입니다...
              </div>
            </div>
          </div>

        </section>

        <section id="sec-goal" class="profile-goal-card">
          <div class="profile-section-head">
            <div class="profile-section-title">목표 설정</div>
            <button
              type="button"
              class="profile-section-edit"
              :aria-pressed="isGoalDetailEditing"
              aria-label="목표 수정"
              @click="
                isGoalDetailEditing
                  ? cancelGoalDetailEdit()
                  : startGoalDetailEdit()
              "
            >
              <i :class="isGoalDetailEditing ? 'pi pi-times' : 'pi pi-pencil'"></i>
            </button>
          </div>

          <GoalTypeSelector
            v-model="profileForm.goalType"
            :options="goalOptions"
            :disabled="!isGoalDetailEditing"
            @select="selectGoalDetail"
          />

          <div class="profile-goal-overview">
            <div>
              <span class="profile-goal-overview-icon calorie">
                <i class="pi pi-apple"></i>
              </span>
              <div>
                <span>목표 섭취 칼로리</span>
                <strong>{{ formatNumber(overviewCalorieGoal) }} kcal</strong>
              </div>
            </div>
            <div>
              <span class="profile-goal-overview-icon exercise">
                <i class="pi pi-fire"></i>
              </span>
              <div>
                <span>목표 소모 칼로리</span>
                <strong>{{ formatNumber(overviewExerciseGoal) }} kcal</strong>
              </div>
            </div>
          </div>

          <div v-if="isGoalDetailEditing" class="profile-goal-inline-editor">
            <label>
              <div class="profile-goal-slider-copy">
                <span>
                  하루 섭취 목표
                  <b class="profile-goal-status" :class="calorieGoalStatus">
                    {{ calorieGoalStatusLabel }}
                  </b>
                </span>
                <p>{{ calorieGoalGuideMessage }}</p>
              </div>
              <strong>{{ formatNumber(displayCalorieGoal) }} kcal</strong>
              <div class="profile-goal-slider" :style="calorieSliderStyle">
                <div class="profile-goal-slider-track"></div>
                <div class="profile-goal-slider-fill"></div>
                <div class="profile-goal-slider-thumb">
                  <i></i>
                  <b>{{ formatNumber(displayCalorieGoal) }}</b>
                </div>
                <input
                  v-model.number="goalDetailForm.calorieIntakeGoal"
                  :max="calorieRange.max"
                  :min="calorieRange.min"
                  step="1"
                  type="range"
                />
              </div>
            </label>
            <label>
              <div class="profile-goal-slider-copy">
                <span>
                  하루 운동 목표
                  <b class="profile-goal-status" :class="exerciseGoalStatus">
                    {{ exerciseGoalStatusLabel }}
                  </b>
                </span>
                <p>{{ exerciseGoalGuideMessage }}</p>
              </div>
              <strong>{{ formatNumber(displayExerciseGoal) }} kcal</strong>
              <div class="profile-goal-slider" :style="exerciseSliderStyle">
                <div class="profile-goal-slider-track"></div>
                <div class="profile-goal-slider-fill"></div>
                <div class="profile-goal-slider-thumb">
                  <i></i>
                  <b>{{ formatNumber(displayExerciseGoal) }}</b>
                </div>
                <input
                  v-model.number="goalDetailForm.exerciseCalorieGoal"
                  :max="exerciseRange.max"
                  :min="exerciseRange.min"
                  step="1"
                  type="range"
                />
              </div>
            </label>
          </div>

          <div v-if="isGoalDetailEditing" class="profile-goal-footer">
            <span>{{ goalFooterMessage }}</span>
            <div>
              <button type="button" class="profile-cancel" @click="cancelGoalDetailEdit">
                취소
              </button>
              <button
                type="button"
                class="profile-save"
                :disabled="dailyGoalStore.isSavingGoal"
                @click="saveGoalDetail"
              >
                {{ dailyGoalStore.isSavingGoal ? "저장 중..." : "목표 저장" }}
              </button>
            </div>
          </div>
        </section>
      </div>
  </div>
</template>
