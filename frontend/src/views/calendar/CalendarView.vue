<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { useAuthStore } from "../../stores/authStore";
import { useExerciseStore } from "../../stores/exerciseStore";
import { useMealStore } from "../../stores/mealStore";
import { useProfileStore } from "../../stores/profileStore";
import { useWeightRecordStore } from "../../stores/weightRecordStore";

const authStore = useAuthStore();
const exerciseStore = useExerciseStore();
const mealStore = useMealStore();
const profileStore = useProfileStore();
const weightRecordStore = useWeightRecordStore();
const router = useRouter();

const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

const mealTypeMeta = {
  BREAKFAST: { label: "아침", className: "breakfast" },
  LUNCH: { label: "점심", className: "lunch" },
  DINNER: { label: "저녁", className: "dinner" },
  SNACK: { label: "간식", className: "snack" },
};

const monthTitle = computed(() => `${mealStore.selectedYear}년 ${mealStore.selectedMonth}월`);
const todayDateKey = computed(() => toDateKey(new Date()));

const calendarCells = computed(() => {
  const year = mealStore.selectedYear;
  const monthIndex = mealStore.selectedMonth - 1;
  const firstDay = new Date(year, monthIndex, 1);
  const lastDay = new Date(year, monthIndex + 1, 0);
  const startDate = new Date(year, monthIndex, 1 - firstDay.getDay());
  const endDate = new Date(year, monthIndex + 1, 6 - lastDay.getDay());
  const cells = [];

  for (const cursor = new Date(startDate); cursor <= endDate; cursor.setDate(cursor.getDate() + 1)) {
    const cellDate = new Date(cursor);
    const dateKey = toDateKey(cellDate);
    const summary = mealStore.monthlyDaysByDate[dateKey];
    const hasExerciseRecord = Boolean(exerciseStore.monthlyDatesByDate[dateKey]);
    const weightRecord = weightRecordStore.calendarRecordsByDate[dateKey];

    cells.push({
      dateKey,
      day: cellDate.getDate(),
      isCurrentMonth: cellDate.getMonth() === monthIndex,
      isToday: dateKey === todayDateKey.value,
      hasExerciseRecord,
      weightRecord,
      summary,
    });
  }

  return cells;
});

onMounted(async () => {
  await Promise.all([
    exerciseStore.loadMonthlyExerciseDates(mealStore.selectedYear, mealStore.selectedMonth),
    mealStore.loadMonthlyMeals(),
    profileStore.loadProfile(),
    weightRecordStore.loadCalendarRecords(mealStore.selectedYear, mealStore.selectedMonth),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
});

async function moveMonth(offset) {
  const nextDate = new Date(mealStore.selectedYear, mealStore.selectedMonth - 1 + offset, 1);
  const year = nextDate.getFullYear();
  const month = nextDate.getMonth() + 1;

  await Promise.all([
    exerciseStore.loadMonthlyExerciseDates(year, month),
    mealStore.loadMonthlyMeals(year, month),
    weightRecordStore.loadCalendarRecords(year, month),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

function openDailyRecord(dateKey) {
  router.push({
    path: "/records",
    query: {
      date: dateKey,
    },
  });
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function visibleMealTypes(summary) {
  return (summary?.mealTypes || []).filter((mealType) => mealTypeMeta[mealType]);
}

function calendarDotLabel(cell) {
  const labels = visibleMealTypes(cell.summary).map((mealType) => mealTypeMeta[mealType].label);

  if (cell.hasExerciseRecord) {
    labels.push("운동");
  }

  if (cell.weightRecord) {
    labels.push("몸무게");
  }

  return labels.length ? `${labels.join(", ")} 기록` : "기록 없음";
}

function formatCalories(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return "";
  }

  return `${Math.round(numberValue).toLocaleString("ko-KR")} kcal`;
}
</script>

<template>
  <main class="calendar-home">
    <AppSidebar />

    <section class="calendar-workspace">
      <header class="calendar-header">
        <div class="calendar-title-block">
          <p class="deco">Your Records</p>
          <div class="calendar-title-row">
            <h1>{{ monthTitle }}</h1>
            <button type="button" aria-label="이전 달" @click="moveMonth(-1)">
              <i class="pi pi-chevron-left"></i>
            </button>
            <button type="button" aria-label="다음 달" @click="moveMonth(1)">
              <i class="pi pi-chevron-right"></i>
            </button>
          </div>
        </div>

        <div class="calendar-legend" aria-label="기록 유형">
          <span v-for="meta in mealTypeMeta" :key="meta.label">
            <i :class="meta.className"></i>
            {{ meta.label }}
          </span>
          <span class="needs-api">
            <i class="exercise"></i>
            운동
          </span>
          <span>
            <i class="weight"></i>
            몸무게
          </span>
        </div>
      </header>

      <section class="calendar-content">
        <div v-if="mealStore.monthlyError" class="calendar-error">
          {{ mealStore.monthlyError }}
        </div>

        <div v-if="exerciseStore.monthlyError" class="calendar-error">
          {{ exerciseStore.monthlyError }}
        </div>

        <div v-if="weightRecordStore.calendarError" class="calendar-error">
          {{ weightRecordStore.calendarError }}
        </div>

        <div
          v-if="mealStore.isLoadingMonthly || exerciseStore.isLoadingMonthly || weightRecordStore.isLoadingCalendarRecords"
          class="calendar-loading"
        >
          월별 기록을 불러오는 중입니다...
        </div>

        <div class="calendar-weekdays">
          <span v-for="weekday in weekdays" :key="weekday" :class="{ sunday: weekday === '일', saturday: weekday === '토' }">
            {{ weekday }}
          </span>
        </div>

        <div class="calendar-grid">
          <div
            v-for="cell in calendarCells"
            :key="cell.dateKey"
            role="button"
            tabindex="0"
            class="calendar-cell"
            :class="{
              muted: !cell.isCurrentMonth,
              recorded: cell.summary || cell.hasExerciseRecord || cell.weightRecord,
              today: cell.isToday,
            }"
            @click="openDailyRecord(cell.dateKey)"
            @keydown.enter="openDailyRecord(cell.dateKey)"
            @keydown.space.prevent="openDailyRecord(cell.dateKey)"
          >
            <div class="calendar-cell-head">
              <strong>{{ cell.day }}</strong>
              <span v-if="cell.isToday">오늘</span>
            </div>

            <p v-if="cell.summary" class="calendar-kcal">
              {{ formatCalories(cell.summary.totalCalories) }}
            </p>

            <p v-if="cell.weightRecord" class="calendar-weight-value">
              {{ cell.weightRecord.weightKg }}kg
            </p>

            <div class="calendar-dots" :aria-label="calendarDotLabel(cell)">
              <i
                v-for="mealType in visibleMealTypes(cell.summary)"
                :key="mealType"
                :class="mealTypeMeta[mealType].className"
                :title="mealTypeMeta[mealType].label"
              ></i>
              <i v-if="cell.hasExerciseRecord" class="exercise" title="운동"></i>
              <i v-if="cell.weightRecord" class="weight" title="몸무게"></i>
            </div>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
