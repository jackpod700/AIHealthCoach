<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
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
const monthMotionClass = ref("");
let monthMotionTimer = null;

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

const calendarWeekCount = computed(() => Math.max(calendarCells.value.length / 7, 1));

const calendarGridStyle = computed(() => ({
  "--calendar-week-count": calendarWeekCount.value,
}));

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

onBeforeUnmount(() => {
  clearMonthMotion();
});

async function moveMonth(offset) {
  startMonthMotion(offset);
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

async function goToday() {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth() + 1;
  const currentDate = new Date(mealStore.selectedYear, mealStore.selectedMonth - 1, 1);
  const todayMonth = new Date(year, month - 1, 1);
  const monthOffset = todayMonth > currentDate ? 1 : todayMonth < currentDate ? -1 : 0;

  startMonthMotion(monthOffset);

  await Promise.all([
    exerciseStore.loadMonthlyExerciseDates(year, month),
    mealStore.loadMonthlyMeals(year, month),
    weightRecordStore.loadCalendarRecords(year, month),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

function startMonthMotion(offset) {
  clearMonthMotion();

  if (!offset) {
    return;
  }

  monthMotionClass.value = offset > 0 ? "moving-next" : "moving-prev";
  monthMotionTimer = window.setTimeout(() => {
    monthMotionClass.value = "";
    monthMotionTimer = null;
  }, 260);
}

function clearMonthMotion() {
  if (monthMotionTimer) {
    window.clearTimeout(monthMotionTimer);
    monthMotionTimer = null;
  }

  monthMotionClass.value = "";
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

</script>

<template>
  <div class="calendar-page">
      <header class="calendar-header">
        <div class="calendar-title-block">
          <div class="calendar-title-row">
            <button type="button" class="calendar-today-button" @click="goToday">
              오늘
            </button>
            <button type="button" aria-label="이전 달" @click="moveMonth(-1)">
              <i class="pi pi-chevron-left"></i>
            </button>
            <h1>{{ monthTitle }}</h1>
            <button type="button" aria-label="다음 달" @click="moveMonth(1)">
              <i class="pi pi-chevron-right"></i>
            </button>
          </div>
        </div>

        <div class="calendar-legend" aria-label="기록 유형">
          <span>
            <i class="meal"></i>
            식사 기록
          </span>
          <span class="needs-api">
            <i class="exercise"></i>
            운동 기록
          </span>
          <span>
            <i class="weight"></i>
            체중 기록
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

        <div class="calendar-board" :class="monthMotionClass">
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

          <div class="calendar-grid" :style="calendarGridStyle">
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
              </div>

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
        </div>
      </section>
  </div>
</template>
