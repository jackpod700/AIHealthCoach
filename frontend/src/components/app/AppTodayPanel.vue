<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useAuthStore } from "../../stores/authStore";
import { useDailyGoalStore } from "../../stores/dailyGoalStore";
import { useMealStore } from "../../stores/mealStore";

const authStore = useAuthStore();
const dailyGoalStore = useDailyGoalStore();
const mealStore = useMealStore();
const showExerciseGoalCelebration = ref(false);
let exerciseGoalCelebrationTimer = null;

const mealTypeMeta = {
  BREAKFAST: { label: "아침", dot: "yellow" },
  LUNCH: { label: "점심", dot: "orange" },
  DINNER: { label: "저녁", dot: "navy" },
  SNACK: { label: "간식", dot: "yellow" },
};

const todayDateKey = computed(() => toDateKey(new Date()));

const todayLabel = computed(() => {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "long",
  }).format(new Date());
});

const todayMeals = computed(() => mealStore.dailyMeal?.meals || []);
const hasTodayMeals = computed(() => todayMeals.value.length > 0);
const dailyGoalProgress = computed(
  () => dailyGoalStore.progress?.progress || null,
);
const calorieProgress = computed(
  () => dailyGoalProgress.value?.calorieIntake || null,
);
const exerciseProgress = computed(
  () => dailyGoalProgress.value?.exerciseCalories || null,
);
const hasDailyGoalProgress = computed(() =>
  Boolean(calorieProgress.value && exerciseProgress.value),
);
const macroRatio = computed(() => dailyGoalStore.progress?.macroRatio || null);
const macroItems = computed(() => {
  const macros = macroRatio.value;

  return [
    { key: "protein", label: "단백질", value: macros?.protein },
    { key: "carbohydrate", label: "탄수화물", value: macros?.carbohydrate },
    { key: "fat", label: "지방", value: macros?.fat },
  ];
});

watch(
  () => exerciseProgress.value?.current,
  (current, previous) => {
    const goal = Number(exerciseProgress.value?.goal);

    if (
      Number.isFinite(goal) &&
      goal > 0 &&
      Number(previous || 0) < goal &&
      Number(current || 0) >= goal
    ) {
      triggerExerciseGoalCelebration();
    }
  },
);

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    return;
  }

  await Promise.all([
    mealStore.loadDailyMeal(todayDateKey.value),
    dailyGoalStore.loadProgress(todayDateKey.value),
  ]);
});

onBeforeUnmount(() => {
  clearExerciseGoalCelebrationTimer();
});

function mealLabel(mealType) {
  return mealTypeMeta[mealType]?.label || mealType;
}

function mealDot(mealType) {
  return mealTypeMeta[mealType]?.dot || "yellow";
}

function mealFoodNames(meal) {
  return (
    meal.items?.map((item) => item.foodName).join(" + ") ||
    mealLabel(meal.mealType)
  );
}

function toNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatNumber(value) {
  return Math.round(toNumber(value)).toLocaleString("ko-KR");
}

function progressWidth(metric) {
  return `${Math.min(Math.max(toNumber(metric?.percent), 0), 100)}%`;
}

function macroStatusClass(macro) {
  return (macro?.status || "LOW").toLowerCase();
}

function triggerExerciseGoalCelebration() {
  showExerciseGoalCelebration.value = true;
  clearExerciseGoalCelebrationTimer();
  exerciseGoalCelebrationTimer = window.setTimeout(() => {
    showExerciseGoalCelebration.value = false;
    exerciseGoalCelebrationTimer = null;
  }, 2600);
}

function clearExerciseGoalCelebrationTimer() {
  if (exerciseGoalCelebrationTimer) {
    window.clearTimeout(exerciseGoalCelebrationTimer);
    exerciseGoalCelebrationTimer = null;
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
  <aside class="today-panel">
    <div class="today-head">
      <p class="deco">Today</p>
      <span>{{ todayLabel }}</span>
    </div>

    <div v-if="dailyGoalStore.progressError" class="api-needed-panel">
      <strong>오늘 목표 진행률을 불러오지 못했어요</strong>
      <p>{{ dailyGoalStore.progressError }}</p>
    </div>

    <div v-else-if="dailyGoalStore.needsGoalSetup" class="api-needed-panel today-goal-empty">
      <i class="pi pi-sliders-h" aria-hidden="true"></i>
      <div>
        <strong>오늘 목표가 비어 있어요</strong>
        <p>채팅창의 목표 설정 카드에서 추천값을 확인하고 오늘 기준으로 저장해 주세요.</p>
      </div>
    </div>

    <section
      class="calorie-card"
      :class="{ 'pending-api': !hasDailyGoalProgress }"
    >
      <span>오늘 섭취 목표</span>
      <strong>
        {{ calorieProgress ? formatNumber(calorieProgress.current) : "-"
        }}<small>/ {{ calorieProgress ? formatNumber(calorieProgress.goal) : "-" }} kcal</small>
      </strong>
      <div class="progress-track">
        <i :style="{ width: progressWidth(calorieProgress) }"></i>
      </div>
      <p v-if="calorieProgress">
        남은 섭취량 {{ formatNumber(calorieProgress.remaining) }} kcal ·
        {{ calorieProgress.percent }}%
      </p>
      <p v-else>목표를 설정하면 진행률을 볼 수 있어요.</p>
    </section>

    <section
      class="exercise-goal-card exercise-card"
      :class="{
        'pending-api': !hasDailyGoalProgress,
        celebrating: showExerciseGoalCelebration,
      }"
    >
      <div
        v-if="showExerciseGoalCelebration"
        class="exercise-goal-bursts"
        aria-hidden="true"
      >
        <i v-for="index in 16" :key="index"></i>
      </div>
      <span>오늘 운동 목표</span>
      <strong>
        {{ exerciseProgress ? formatNumber(exerciseProgress.current) : "-"
        }}<small>/ {{ exerciseProgress ? formatNumber(exerciseProgress.goal) : "-" }} kcal</small>
      </strong>
      <div class="progress-track">
        <i :style="{ width: progressWidth(exerciseProgress) }"></i>
      </div>
      <p v-if="exerciseProgress">
        남은 운동량 {{ formatNumber(exerciseProgress.remaining) }} kcal ·
        {{ exerciseProgress.percent }}%
      </p>
      <p v-else>운동 목표도 함께 추적해요.</p>
    </section>

    <div v-if="mealStore.dailyError" class="api-needed-panel compact-panel">
      <strong>오늘 식단 정보를 불러오지 못했어요</strong>
      <p>{{ mealStore.dailyError }}</p>
    </div>

    <div class="macro-grid" :class="{ 'pending-api': !macroRatio }">
      <div
        v-for="macro in macroItems"
        :key="macro.key"
        :class="macroStatusClass(macro.value)"
      >
        <span>{{ macro.label }}</span>
        <strong>
          {{ macro.value ? formatNumber(macro.value.grams) : "-" }}g
          <small>{{ macro.value ? `${macro.value.percent}%` : "" }}</small>
        </strong>
        <i :style="{ width: progressWidth(macro.value) }"></i>
      </div>
    </div>

    <section class="today-log">
      <h2>오늘의 기록</h2>

      <article v-for="meal in todayMeals" :key="meal.mealId">
        <time>{{ mealLabel(meal.mealType) }}</time>
        <i :class="['dot', mealDot(meal.mealType)]"></i>
        <div>
          <strong>{{ mealFoodNames(meal) }}</strong>
          <span>
            {{ mealLabel(meal.mealType) }} ·
            {{ formatNumber(meal.totalCalories) }} kcal
          </span>
        </div>
      </article>

      <div
        v-if="!mealStore.isLoadingDaily && !hasTodayMeals"
        class="api-list-empty today-meal-empty"
      >
        <i class="pi pi-bookmark" aria-hidden="true"></i>
        <div>
          <strong>아직 기록된 식단이 없어요</strong>
          <span>채팅이나 기록 화면에서 오늘의 첫 끼니를 남겨보세요.</span>
        </div>
      </div>
    </section>
  </aside>
</template>
