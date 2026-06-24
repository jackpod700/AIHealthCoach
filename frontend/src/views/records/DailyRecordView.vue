<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
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
const route = useRoute();
const router = useRouter();

const foodQuery = ref("");
const exerciseQuery = ref("");
const addMenuOpen = ref(false);
const searchRequestId = ref(0);
const exerciseSearchRequestId = ref(0);
let foodSearchTimer = null;
let exerciseSearchTimer = null;
const editForm = reactive({
  open: false,
  mealId: null,
  mealType: "BREAKFAST",
  items: [],
});
const exerciseEditForm = reactive({
  open: false,
  recordId: null,
  exerciseActivityOptionId: null,
  activityNameKo: "",
  intensityLevel: "MEDIUM",
  exerciseDate: "",
  durationMinutes: 30,
  memo: "",
});
const weightEditForm = reactive({
  open: false,
  recordDate: "",
  weightKg: "",
});

const mealTypeMeta = {
  BREAKFAST: { label: "아침", icon: "pi pi-sun", className: "breakfast", order: 1 },
  LUNCH: { label: "점심", icon: "pi pi-apple", className: "lunch", order: 2 },
  DINNER: { label: "저녁", icon: "pi pi-moon", className: "dinner", order: 3 },
  SNACK: { label: "간식", icon: "pi pi-star", className: "snack", order: 4 },
};

const selectedDate = computed(() => {
  return normalizeDateQuery(route.query.date) || toDateKey(new Date());
});

const selectedDateLabel = computed(() => {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "long",
  }).format(parseDateKey(selectedDate.value));
});

const isToday = computed(() => selectedDate.value === toDateKey(new Date()));
const todayDateKey = computed(() => toDateKey(new Date()));

const meals = computed(() => {
  return [...(mealStore.dailyMeal?.meals || [])].sort((a, b) => {
    const left = mealTypeMeta[a.mealType]?.order || 99;
    const right = mealTypeMeta[b.mealType]?.order || 99;

    return left - right;
  });
});

const recordCount = computed(() => meals.value.length);
const exerciseRecords = computed(() => exerciseStore.dailyRecords || []);
const exerciseCount = computed(() => exerciseRecords.value.length);
const weightRecord = computed(() => weightRecordStore.calendarRecordsByDate[selectedDate.value] || null);
const weightRecordCount = computed(() => (weightRecord.value ? 1 : 0));
const totalRecordCount = computed(() => recordCount.value + exerciseCount.value + weightRecordCount.value);
const isDailyRecordLoading = computed(
  () => mealStore.isLoadingDaily || exerciseStore.isLoadingDaily,
);

const dailyTotals = computed(() => {
  const dailyMeal = mealStore.dailyMeal;

  return {
    calories: toNumber(dailyMeal?.dailyTotalCalories),
    carbohydrate: toNumber(dailyMeal?.dailyTotalCarbohydrate),
    protein: toNumber(dailyMeal?.dailyTotalProtein),
    fat: toNumber(dailyMeal?.dailyTotalFat),
  };
});

const canSaveMeal = computed(() => {
  return editForm.items.length > 0 && editForm.items.every((item) => Number(item.quantity) > 0);
});

const canSaveExercise = computed(() => {
  return Boolean(
      exerciseEditForm.exerciseActivityOptionId &&
      /^\d{4}-\d{2}-\d{2}$/.test(exerciseEditForm.exerciseDate) &&
      ["LOW", "MEDIUM", "HIGH"].includes(exerciseEditForm.intensityLevel) &&
      Number(exerciseEditForm.durationMinutes) > 0
  );
});

const canSaveWeight = computed(() => {
  const weightKg = Number(weightEditForm.weightKg);

  return (
    /^\d{4}-\d{2}-\d{2}$/.test(weightEditForm.recordDate) &&
    weightEditForm.recordDate <= todayDateKey.value &&
    Number.isFinite(weightKg) &&
    weightKg > 0 &&
    weightKg <= 500
  );
});

onMounted(async () => {
  await Promise.all([
    exerciseStore.loadDailyExerciseRecords(selectedDate.value),
    mealStore.loadDailyMeal(selectedDate.value),
    profileStore.loadProfile(),
    loadWeightRecordsForSelectedMonth(selectedDate.value),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
});

watch(selectedDate, async (date) => {
  closeEditMeal();
  closeEditExercise();
  closeEditWeight();
  addMenuOpen.value = false;
  await Promise.all([
    exerciseStore.loadDailyExerciseRecords(date),
    mealStore.loadDailyMeal(date),
    loadWeightRecordsForSelectedMonth(date),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
});

watch(foodQuery, (query) => {
  const currentRequestId = searchRequestId.value + 1;
  searchRequestId.value = currentRequestId;

  window.clearTimeout(foodSearchTimer);
  foodSearchTimer = window.setTimeout(async () => {
    if (searchRequestId.value !== currentRequestId) {
      return;
    }

    await mealStore.searchMealFoods(query);
  }, 250);
});

watch(exerciseQuery, (query) => {
  const currentRequestId = exerciseSearchRequestId.value + 1;
  exerciseSearchRequestId.value = currentRequestId;

  window.clearTimeout(exerciseSearchTimer);
  exerciseSearchTimer = window.setTimeout(async () => {
    if (exerciseSearchRequestId.value !== currentRequestId) {
      return;
    }

    await exerciseStore.searchActivities(query);
  }, 250);
});

function openEditMeal(meal) {
  editForm.open = true;
  editForm.mealId = meal.mealId;
  editForm.mealType = meal.mealType;
  editForm.items = meal.items.map((item) => ({
    foodId: item.foodId,
    foodName: item.foodName,
    brand: item.brand,
    servingSize: item.servingSize,
    servingUnit: item.servingUnit,
    calories: item.calories,
    baseCalories: baseCalories(item),
    quantity: Number(item.quantity || 1),
  }));
  foodQuery.value = "";
  mealStore.clearFoodSearch();

  nextTick(() => {
    document.querySelector(".meal-edit-modal input")?.focus();
  });
}

function openAddMeal() {
  editForm.open = true;
  editForm.mealId = null;
  editForm.mealType = "BREAKFAST";
  editForm.items = [];
  foodQuery.value = "";
  mealStore.clearFoodSearch();
  addMenuOpen.value = false;

  nextTick(() => {
    document.querySelector(".meal-edit-modal input")?.focus();
  });
}

function closeEditMeal() {
  editForm.open = false;
  editForm.mealId = null;
  editForm.mealType = "BREAKFAST";
  editForm.items = [];
  foodQuery.value = "";
  mealStore.clearFoodSearch();
}

function openEditExercise(record) {
  exerciseEditForm.open = true;
  exerciseEditForm.recordId = record.id;
  exerciseEditForm.exerciseActivityOptionId = record.exerciseActivityOptionId;
  exerciseEditForm.activityNameKo = record.activityNameKo;
  exerciseEditForm.intensityLevel = record.intensityLevel || "MEDIUM";
  exerciseEditForm.exerciseDate = record.exerciseDate || selectedDate.value;
  exerciseEditForm.durationMinutes = Number(record.durationMinutes || 30);
  exerciseEditForm.memo = record.memo || "";
  exerciseQuery.value = record.activityNameKo || "";
  exerciseStore.clearActivitySearch();

  nextTick(() => {
    document.querySelector(".exercise-edit-modal input")?.focus();
  });
}

function openAddExercise() {
  exerciseEditForm.open = true;
  exerciseEditForm.recordId = null;
  exerciseEditForm.exerciseActivityOptionId = null;
  exerciseEditForm.activityNameKo = "";
  exerciseEditForm.intensityLevel = "MEDIUM";
  exerciseEditForm.exerciseDate = selectedDate.value;
  exerciseEditForm.durationMinutes = 30;
  exerciseEditForm.memo = "";
  exerciseQuery.value = "";
  exerciseStore.clearActivitySearch();
  addMenuOpen.value = false;

  nextTick(() => {
    document.querySelector(".exercise-edit-modal input")?.focus();
  });
}

function closeEditExercise() {
  exerciseEditForm.open = false;
  exerciseEditForm.recordId = null;
  exerciseEditForm.exerciseActivityOptionId = null;
  exerciseEditForm.activityNameKo = "";
  exerciseEditForm.intensityLevel = "MEDIUM";
  exerciseEditForm.exerciseDate = "";
  exerciseEditForm.durationMinutes = 30;
  exerciseEditForm.memo = "";
  exerciseQuery.value = "";
  exerciseStore.clearActivitySearch();
}

function selectExerciseActivity(activity) {
  exerciseEditForm.exerciseActivityOptionId = activity.id;
  exerciseEditForm.activityNameKo = activity.activityNameKo;
  exerciseQuery.value = activity.activityNameKo;
  exerciseStore.clearActivitySearch();
}

function openAddWeight() {
  weightEditForm.open = true;
  weightEditForm.recordDate = selectedDate.value;
  weightEditForm.weightKg = weightRecord.value?.weightKg ?? profileStore.profile?.currentWeightKg ?? "";
  weightRecordStore.saveError = "";
  addMenuOpen.value = false;
}

function closeEditWeight() {
  weightEditForm.open = false;
  weightEditForm.recordDate = "";
  weightEditForm.weightKg = "";
  weightRecordStore.saveError = "";
}

function addFood(food) {
  const existingItem = editForm.items.find((item) => item.foodId === food.foodId);

  if (existingItem) {
    existingItem.quantity = roundQuantity(Number(existingItem.quantity) + 1);
  } else {
    editForm.items.push({
      foodId: food.foodId,
      foodName: food.foodName,
      brand: food.brand,
      servingSize: food.servingSize,
      servingUnit: food.servingUnit,
      calories: food.calories,
      baseCalories: food.calories,
      quantity: 1,
    });
  }

  foodQuery.value = "";
  mealStore.clearFoodSearch();
}

function removeFood(foodId) {
  editForm.items = editForm.items.filter((item) => item.foodId !== foodId);
}

function updateQuantity(item, value) {
  item.quantity = roundQuantity(value);
}

async function saveEditedMeal() {
  if (!canSaveMeal.value) {
    return;
  }

  const saved = await mealStore.saveMealItems({
    mealDate: selectedDate.value,
    mealType: editForm.mealType,
    items: editForm.items.map((item) => ({
      foodId: item.foodId,
      quantity: Number(item.quantity),
    })),
  });

  if (saved) {
    closeEditMeal();
    await refreshSelectedDateRecords();
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

async function deleteEditedMeal() {
  if (!editForm.mealId) {
    return;
  }

  if (!window.confirm(`${mealMeta(editForm.mealType).label} 끼니를 삭제할까요?`)) {
    return;
  }

  const deleted = await mealStore.deleteMealById(editForm.mealId, selectedDate.value);

  if (deleted) {
    closeEditMeal();
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

async function saveEditedExercise() {
  if (!canSaveExercise.value) {
    return;
  }

  const targetDate = exerciseEditForm.exerciseDate;
  const payload = {
    exerciseActivityOptionId: exerciseEditForm.exerciseActivityOptionId,
    intensityLevel: exerciseEditForm.intensityLevel,
    durationMinutes: Number(exerciseEditForm.durationMinutes),
    memo: exerciseEditForm.memo,
  };
  if (!exerciseEditForm.recordId) {
    payload.exerciseDate = targetDate;
  }
  const saved = exerciseEditForm.recordId
    ? await exerciseStore.updateRecord(exerciseEditForm.recordId, payload, targetDate)
    : await exerciseStore.saveRecord(payload);

  if (saved) {
    closeEditExercise();
    await refreshSelectedDateRecords();

    if (targetDate !== selectedDate.value) {
      router.replace({
        path: "/records",
        query: {
          date: targetDate,
        },
      });
    }
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

async function deleteEditedExercise() {
  if (!exerciseEditForm.recordId) {
    return;
  }

  if (!window.confirm(`${exerciseEditForm.activityNameKo || "운동"} 기록을 삭제할까요?`)) {
    return;
  }

  const deleted = await exerciseStore.deleteRecord(exerciseEditForm.recordId, selectedDate.value);

  if (deleted) {
    closeEditExercise();
    await refreshSelectedDateRecords();
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}

async function saveEditedWeight() {
  if (!canSaveWeight.value) {
    return;
  }

  const targetDate = weightEditForm.recordDate;
  const saved = await weightRecordStore.saveRecord({
    recordDate: targetDate,
    weightKg: Number(weightEditForm.weightKg),
  });

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (!saved) {
    return;
  }

  closeEditWeight();
  await Promise.all([
    loadWeightRecordsForSelectedMonth(targetDate),
    profileStore.loadProfile(),
  ]);

  if (targetDate !== selectedDate.value) {
    router.replace({
      path: "/records",
      query: {
        date: targetDate,
      },
    });
  }
}

async function refreshSelectedDateRecords() {
  await Promise.all([
    exerciseStore.loadDailyExerciseRecords(selectedDate.value),
    mealStore.loadDailyMeal(selectedDate.value),
    loadWeightRecordsForSelectedMonth(selectedDate.value),
  ]);
}

function toggleAddMenu() {
  addMenuOpen.value = !addMenuOpen.value;
}

function loadWeightRecordsForSelectedMonth(dateKey) {
  const [year, month] = dateKey.split("-").map(Number);
  return weightRecordStore.loadCalendarRecords(year, month);
}

function normalizeDateQuery(value) {
  if (typeof value !== "string") {
    return "";
  }

  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : "";
}

function parseDateKey(dateKey) {
  const [year, month, day] = dateKey.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function toNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function roundQuantity(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return 0.1;
  }

  return Math.round(numberValue * 10) / 10;
}

function formatNumber(value) {
  return Math.round(toNumber(value)).toLocaleString("ko-KR");
}

function formatMet(value) {
  const numberValue = toNumber(value);

  return numberValue.toLocaleString("ko-KR", {
    maximumFractionDigits: 1,
  });
}

function mealMeta(mealType) {
  return mealTypeMeta[mealType] || { label: mealType, icon: "pi pi-circle", className: "snack" };
}

function itemCalories(item) {
  return `${formatNumber(item.calories)} kcal`;
}

function exerciseIntensityLabel(intensityLevel) {
  const labels = {
    LOW: "하",
    MEDIUM: "중",
    HIGH: "상",
  };

  return labels[intensityLevel] || intensityLevel;
}

function exerciseMeta(record) {
  return [
    exerciseIntensityLabel(record.intensityLevel),
    `${record.durationMinutes}분`,
    `${formatMet(record.metValue)} MET`,
  ].join(" · ");
}

function exerciseActivityMeta(activity) {
  const metValues = [activity.low?.metValue, activity.medium?.metValue, activity.high?.metValue]
    .map(toNumber)
    .filter((value) => value > 0);

  if (!metValues.length) {
    return activity.majorHeading || "운동 옵션";
  }

  return `${activity.majorHeading || "운동 옵션"} · ${metValues.map((value) => `${formatMet(value)} MET`).join(" / ")}`;
}

function baseCalories(item) {
  const quantity = toNumber(item.quantity);

  if (quantity <= 0) {
    return toNumber(item.calories);
  }

  return toNumber(item.calories) / quantity;
}

function servingLabel(item) {
  const size = item.servingSize ? `${item.servingSize}` : "1";
  const unit = item.servingUnit || "회";

  return `${size}${unit} 기준`;
}

function servingCalorieLabel(item) {
  return `${servingLabel(item)} · ${formatNumber(item.baseCalories ?? baseCalories(item))} kcal`;
}

</script>

<template>
      <header class="record-header">
        <div>
          <p class="deco">Daily Log</p>
          <h1>{{ selectedDateLabel }}</h1>
        </div>
        <div class="streak-chip">
          <i></i>
          {{ isToday ? "오늘" : "선택 날짜" }} · 기록 {{ totalRecordCount }}건
        </div>
      </header>

      <section class="record-content">
        <div v-if="mealStore.dailyError" class="record-error">
          {{ mealStore.dailyError }}
        </div>

        <div v-if="exerciseStore.dailyError" class="record-error">
          {{ exerciseStore.dailyError }}
        </div>

        <div v-if="isDailyRecordLoading" class="record-loading">
          일일 기록을 불러오는 중입니다...
        </div>

        <section class="record-summary-card">
          <div>
            <span>섭취</span>
            <strong>{{ formatNumber(dailyTotals.calories) }}<small>kcal</small></strong>
          </div>
          <div>
            <span>운동 소모</span>
            <strong>{{ formatNumber(exerciseStore.dailyCaloriesBurned) }}<small>kcal</small></strong>
          </div>
          <div>
            <span>단백질</span>
            <strong>{{ formatNumber(dailyTotals.protein) }}<small>g</small></strong>
          </div>
          <div>
            <span>탄수화물</span>
            <strong>{{ formatNumber(dailyTotals.carbohydrate) }}<small>g</small></strong>
          </div>
          <div>
            <span>지방</span>
            <strong>{{ formatNumber(dailyTotals.fat) }}<small>g</small></strong>
          </div>
        </section>

        <div class="record-section-title">오늘의 기록</div>

        <div class="record-timeline-scroll">
          <section class="record-timeline">
            <article
              v-if="totalRecordCount === 0 && !isDailyRecordLoading"
              class="record-timeline-row record-empty-row"
            >
              <div class="record-time"></div>
              <div class="record-line">
                <span class="record-icon disabled">
                  <i class="pi pi-calendar-plus"></i>
                </span>
              </div>
              <div class="record-card muted-card record-empty-card">
                <strong>아직 오늘의 기록이 없습니다.</strong>
                <span>오늘의 기록을 추가해보아요.</span>
              </div>
            </article>

            <article v-if="recordCount" class="record-timeline-row">
              <div class="record-time">식사</div>
              <div class="record-line">
                <span class="record-icon meal">
                  <i class="pi pi-apple"></i>
                </span>
              </div>
              <div class="record-card">
                <div class="record-card-head">
                  <div>
                    <strong>식사</strong>
                    <span>{{ recordCount }}개 기록</span>
                  </div>
                  <em>{{ formatNumber(dailyTotals.calories) }} kcal</em>
                </div>

                <div class="record-food-list">
                  <button
                    v-for="meal in meals"
                    :key="meal.mealId"
                    type="button"
                    class="record-food-item"
                    @click="openEditMeal(meal)"
                  >
                    <span>{{ mealMeta(meal.mealType).label }}</span>
                    <strong>{{ meal.items.map((item) => item.foodName).join(" + ") }}</strong>
                    <em>{{ formatNumber(meal.totalCalories) }} kcal</em>
                    <i class="pi pi-pencil record-edit-cue"></i>
                  </button>
                </div>
              </div>
            </article>

            <article v-if="exerciseCount" class="record-timeline-row">
              <div class="record-time">운동</div>
              <div class="record-line">
                <span class="record-icon exercise">
                  <i class="pi pi-bolt"></i>
                </span>
              </div>
              <div class="record-card">
                <div class="record-card-head">
                  <div>
                    <strong>운동</strong>
                    <span>{{ exerciseCount }}개 기록</span>
                  </div>
                  <em>{{ formatNumber(exerciseStore.dailyCaloriesBurned) }} kcal</em>
                </div>

                <div class="record-exercise-list">
                  <button
                    v-for="record in exerciseRecords"
                    :key="record.id"
                    type="button"
                    class="record-exercise-item"
                    @click="openEditExercise(record)"
                  >
                    <div>
                      <strong>{{ record.activityNameKo }}</strong>
                    </div>
                    <em>{{ formatNumber(record.caloriesBurned) }} kcal</em>
                    <i class="pi pi-pencil record-edit-cue"></i>
                  </button>
                </div>
              </div>
            </article>

            <article v-if="weightRecord" class="record-timeline-row">
              <div class="record-time">몸무게</div>
              <div class="record-line">
                <span class="record-icon weight">
                  <i class="pi pi-heart"></i>
                </span>
              </div>
              <button
                class="record-card editable weight-record-card"
                type="button"
                @click="openAddWeight"
              >
                <div class="record-card-head">
                  <div>
                    <strong>몸무게</strong>
                    <span>1개 기록</span>
                  </div>
                  <em>{{ weightRecord.weightKg }} kg</em>
                </div>
                <i class="pi pi-pencil record-edit-cue"></i>
              </button>
            </article>

            <article class="record-timeline-row record-add-row" :class="{ open: addMenuOpen }">
              <div class="record-time"></div>
              <div class="record-line">
                <button
                  type="button"
                  class="record-add-toggle"
                  :class="{ open: addMenuOpen }"
                  aria-label="기록 추가"
                  @click="toggleAddMenu"
                >
                  <i class="pi pi-plus"></i>
                </button>
              </div>
              <div class="record-add-panel" :class="{ open: addMenuOpen }">
                <button type="button" @click="openAddMeal">
                  <i class="record-add-icon meal"></i>
                  식사
                </button>
                <button type="button" @click="openAddExercise">
                  <i class="record-add-icon exercise"></i>
                  운동
                </button>
                <button type="button" @click="openAddWeight">
                  <i class="record-add-icon weight"></i>
                  몸무게 기록
                </button>
              </div>
            </article>
          </section>
        </div>
      </section>
    <div v-if="editForm.open" class="meal-edit-backdrop" @click.self="closeEditMeal">
      <section class="meal-edit-modal" role="dialog" aria-modal="true" aria-label="끼니 수정">
        <header>
          <div>
            <p class="deco">Edit Meal</p>
            <h2>{{ editForm.mealId ? `${mealMeta(editForm.mealType).label} 수정` : "식사 추가" }}</h2>
          </div>
          <button type="button" aria-label="닫기" @click="closeEditMeal">
            <i class="pi pi-times"></i>
          </button>
        </header>

        <div class="meal-edit-search">
          <div v-if="!editForm.mealId" class="meal-type-picker">
            <button
              v-for="(meta, mealType) in mealTypeMeta"
              :key="mealType"
              type="button"
              :class="{ selected: editForm.mealType === mealType }"
              @click="editForm.mealType = mealType"
            >
              <i :class="meta.icon"></i>
              {{ meta.label }}
            </button>
          </div>

          <label>
            <span>음식 검색</span>
            <input v-model="foodQuery" placeholder="음식명이나 제조사명을 입력하세요" />
          </label>

          <div v-if="mealStore.isSearchingFoods" class="food-search-state">
            검색 중입니다...
          </div>

          <div v-else-if="mealStore.foodSearchError" class="food-search-state error">
            {{ mealStore.foodSearchError }}
          </div>

          <div v-else-if="mealStore.foodSearchResults.length" class="food-search-results-wrap">
            <p class="food-search-count">
              총 {{ formatNumber(mealStore.foodSearchTotalItems) }}개 중 {{ formatNumber(mealStore.foodSearchResults.length) }}개 표시
            </p>
            <div class="food-search-results">
              <button
                v-for="food in mealStore.foodSearchResults"
                :key="food.foodId"
                type="button"
                @click="addFood(food)"
              >
                <strong>{{ food.foodName }}</strong>
                <span>{{ food.brand || "제조사 정보 없음" }} · {{ servingLabel(food) }} · {{ formatNumber(food.calories) }} kcal</span>
              </button>
            </div>
          </div>
        </div>

        <div class="meal-edit-items">
          <article v-for="item in editForm.items" :key="item.foodId" class="meal-edit-item">
            <div>
              <strong>{{ item.foodName }}</strong>
              <span>{{ item.brand || "제조사 정보 없음" }} · {{ servingCalorieLabel(item) }}</span>
            </div>
            <label>
              <span>배수</span>
              <input
                :value="item.quantity"
                type="number"
                min="0.1"
                step="0.1"
                @input="updateQuantity(item, $event.target.value)"
              />
            </label>
            <button type="button" aria-label="음식 제거" @click="removeFood(item.foodId)">
              <i class="pi pi-trash"></i>
            </button>
          </article>
        </div>

        <p v-if="mealStore.saveMealError || mealStore.deleteMealError" class="meal-edit-error">
          {{ mealStore.saveMealError || mealStore.deleteMealError }}
        </p>

        <footer>
          <button
            v-if="editForm.mealId"
            class="meal-edit-delete"
            type="button"
            :disabled="mealStore.isDeletingMeal || mealStore.isSavingMeal"
            @click="deleteEditedMeal"
          >
            {{ mealStore.isDeletingMeal ? "삭제 중..." : "삭제" }}
          </button>
          <div>
            <button class="meal-edit-cancel" type="button" @click="closeEditMeal">취소</button>
            <button
              class="meal-edit-save"
              type="button"
              :disabled="!canSaveMeal || mealStore.isSavingMeal || mealStore.isDeletingMeal"
              @click="saveEditedMeal"
            >
              {{ mealStore.isSavingMeal ? "저장 중..." : editForm.mealId ? "수정 저장" : "기록 추가" }}
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div v-if="exerciseEditForm.open" class="meal-edit-backdrop" @click.self="closeEditExercise">
      <section class="meal-edit-modal exercise-edit-modal" role="dialog" aria-modal="true" aria-label="운동 수정">
        <header>
          <div>
            <p class="deco">Edit Exercise</p>
            <h2>{{ exerciseEditForm.recordId ? "운동 수정" : "운동 추가" }}</h2>
          </div>
          <button type="button" aria-label="닫기" @click="closeEditExercise">
            <i class="pi pi-times"></i>
          </button>
        </header>

        <div class="meal-edit-search exercise-edit-search">
          <label>
            <span>운동 검색</span>
            <input v-model="exerciseQuery" placeholder="운동명을 입력하세요" />
          </label>

          <div v-if="exerciseEditForm.activityNameKo" class="exercise-selected-activity">
            <strong>{{ exerciseEditForm.activityNameKo }}</strong>
            <span>선택된 운동</span>
          </div>

          <div v-if="exerciseStore.isSearchingActivities" class="food-search-state">
            검색 중입니다...
          </div>

          <div v-else-if="exerciseStore.activitySearchError" class="food-search-state error">
            {{ exerciseStore.activitySearchError }}
          </div>

          <div v-else-if="exerciseStore.activitySearchResults.length" class="food-search-results exercise-search-results">
            <button
              v-for="activity in exerciseStore.activitySearchResults"
              :key="activity.id"
              type="button"
              @click="selectExerciseActivity(activity)"
            >
              <strong>{{ activity.activityNameKo }}</strong>
              <span>{{ exerciseActivityMeta(activity) }}</span>
            </button>
          </div>
        </div>

        <div class="exercise-edit-fields">
          <label>
            <span>강도</span>
            <div class="exercise-intensity-options">
              <button
                v-for="level in ['LOW', 'MEDIUM', 'HIGH']"
                :key="level"
                type="button"
                :class="{ selected: exerciseEditForm.intensityLevel === level }"
                @click="exerciseEditForm.intensityLevel = level"
              >
                {{ exerciseIntensityLabel(level) }}
              </button>
            </div>
          </label>

          <label>
            <span>운동 날짜</span>
            <input v-model="exerciseEditForm.exerciseDate" type="date" :disabled="Boolean(exerciseEditForm.recordId)" />
          </label>

          <label>
            <span>운동 시간</span>
            <input v-model="exerciseEditForm.durationMinutes" type="number" min="1" step="1" />
          </label>

          <label class="exercise-memo-field">
            <span>메모</span>
            <input v-model="exerciseEditForm.memo" placeholder="메모를 입력하세요" />
          </label>
        </div>

        <p v-if="exerciseStore.saveRecordError || exerciseStore.deleteRecordError" class="meal-edit-error">
          {{ exerciseStore.saveRecordError || exerciseStore.deleteRecordError }}
        </p>

        <footer>
          <button
            v-if="exerciseEditForm.recordId"
            class="meal-edit-delete"
            type="button"
            :disabled="exerciseStore.isDeletingRecord || exerciseStore.isSavingRecord"
            @click="deleteEditedExercise"
          >
            {{ exerciseStore.isDeletingRecord ? "삭제 중..." : "삭제" }}
          </button>
          <div>
            <button class="meal-edit-cancel" type="button" @click="closeEditExercise">취소</button>
            <button
              class="meal-edit-save"
              type="button"
              :disabled="!canSaveExercise || exerciseStore.isSavingRecord || exerciseStore.isDeletingRecord"
              @click="saveEditedExercise"
            >
              {{ exerciseStore.isSavingRecord ? "저장 중..." : exerciseEditForm.recordId ? "수정 저장" : "기록 추가" }}
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div v-if="weightEditForm.open" class="meal-edit-backdrop" @click.self="closeEditWeight">
      <section class="meal-edit-modal weight-edit-modal" role="dialog" aria-modal="true" aria-label="몸무게 기록">
        <header>
          <div>
            <p class="deco">Edit Weight</p>
            <h2>{{ weightRecord ? "몸무게 수정" : "몸무게 추가" }}</h2>
          </div>
          <button type="button" aria-label="닫기" @click="closeEditWeight">
            <i class="pi pi-times"></i>
          </button>
        </header>

        <div class="weight-edit-fields">
          <label>
            <span>기록 날짜</span>
            <input v-model="weightEditForm.recordDate" :max="todayDateKey" type="date" />
          </label>

          <label>
            <span>몸무게 (kg)</span>
            <input v-model="weightEditForm.weightKg" inputmode="decimal" max="500" min="0.01" step="0.1" type="number" />
          </label>
        </div>

        <p v-if="weightRecordStore.saveError" class="meal-edit-error">
          {{ weightRecordStore.saveError }}
        </p>

        <footer>
          <span></span>
          <div>
            <button class="meal-edit-cancel" type="button" @click="closeEditWeight">취소</button>
            <button
              class="meal-edit-save"
              type="button"
              :disabled="!canSaveWeight || weightRecordStore.isSavingRecord"
              @click="saveEditedWeight"
            >
              {{ weightRecordStore.isSavingRecord ? "저장 중..." : "저장" }}
            </button>
          </div>
        </footer>
      </section>
    </div>
</template>
