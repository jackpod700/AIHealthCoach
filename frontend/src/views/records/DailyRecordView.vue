<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { useAuthStore } from "../../stores/authStore";
import { useExerciseStore } from "../../stores/exerciseStore";
import { useMealStore } from "../../stores/mealStore";
import { useProfileStore } from "../../stores/profileStore";

const authStore = useAuthStore();
const exerciseStore = useExerciseStore();
const mealStore = useMealStore();
const profileStore = useProfileStore();
const route = useRoute();
const router = useRouter();

const foodQuery = ref("");
const exerciseQuery = ref("");
const searchRequestId = ref(0);
const exerciseSearchRequestId = ref(0);
let foodSearchTimer = null;
let exerciseSearchTimer = null;
const editForm = reactive({
  open: false,
  mealId: null,
  mealType: "",
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
const totalRecordCount = computed(() => recordCount.value + exerciseCount.value);
const hasRecords = computed(() => totalRecordCount.value > 0);

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
    exerciseEditForm.recordId &&
      exerciseEditForm.exerciseActivityOptionId &&
      /^\d{4}-\d{2}-\d{2}$/.test(exerciseEditForm.exerciseDate) &&
      ["LOW", "MEDIUM", "HIGH"].includes(exerciseEditForm.intensityLevel) &&
      Number(exerciseEditForm.durationMinutes) > 0
  );
});

onMounted(async () => {
  await Promise.all([
    exerciseStore.loadDailyExerciseRecords(selectedDate.value),
    mealStore.loadDailyMeal(selectedDate.value),
    profileStore.loadProfile(),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
});

watch(selectedDate, async (date) => {
  closeEditMeal();
  closeEditExercise();
  await Promise.all([
    exerciseStore.loadDailyExerciseRecords(date),
    mealStore.loadDailyMeal(date),
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
    foodCode: item.foodCode,
    foodName: item.foodName,
    manufacturer: item.manufacturer,
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

function closeEditMeal() {
  editForm.open = false;
  editForm.mealId = null;
  editForm.mealType = "";
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

function addFood(food) {
  const existingItem = editForm.items.find((item) => item.foodCode === food.foodCode);

  if (existingItem) {
    existingItem.quantity = roundQuantity(Number(existingItem.quantity) + 1);
  } else {
    editForm.items.push({
      foodCode: food.foodCode,
      foodName: food.foodName,
      manufacturer: food.manufacturer,
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

function removeFood(foodCode) {
  editForm.items = editForm.items.filter((item) => item.foodCode !== foodCode);
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
      foodCode: item.foodCode,
      quantity: Number(item.quantity),
    })),
  });

  if (saved) {
    closeEditMeal();
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
  const saved = await exerciseStore.updateRecord(exerciseEditForm.recordId, {
    exerciseActivityOptionId: exerciseEditForm.exerciseActivityOptionId,
    intensityLevel: exerciseEditForm.intensityLevel,
    exerciseDate: targetDate,
    durationMinutes: Number(exerciseEditForm.durationMinutes),
    memo: exerciseEditForm.memo,
  });

  if (saved) {
    closeEditExercise();

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

function goToChat() {
  router.push("/chat");
}
</script>

<template>
  <main class="record-home">
    <AppSidebar />

    <section class="record-workspace">
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

        <div v-if="mealStore.isLoadingDaily || exerciseStore.isLoadingDaily" class="record-loading">
          일일 기록을 불러오는 중입니다...
        </div>

        <template v-else-if="hasRecords">
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
              <span>총 칼로리</span>
              <strong>{{ formatNumber(dailyTotals.calories - exerciseStore.dailyCaloriesBurned) }}<small>kcal</small></strong>
            </div>
            <div>
              <span>단백질 · 탄수 · 지방</span>
              <strong>
                {{ formatNumber(dailyTotals.protein) }} · {{ formatNumber(dailyTotals.carbohydrate) }} · {{ formatNumber(dailyTotals.fat) }}<small>g</small>
              </strong>
            </div>
          </section>

          <section class="record-timeline">
            <article v-for="meal in meals" :key="meal.mealId" class="record-timeline-row">
              <div class="record-time">{{ mealMeta(meal.mealType).label }}</div>
              <div class="record-line">
                <span :class="['record-icon', mealMeta(meal.mealType).className]">
                  <i :class="mealMeta(meal.mealType).icon"></i>
                </span>
              </div>
              <button class="record-card editable" type="button" @click="openEditMeal(meal)">
                <div class="record-card-head">
                  <div>
                    <strong>{{ mealMeta(meal.mealType).label }}</strong>
                    <span>클릭해서 수정</span>
                  </div>
                  <em>{{ formatNumber(meal.totalCalories) }} kcal</em>
                </div>

                <div class="record-food-list">
              <span v-for="item in meal.items" :key="`${meal.mealId}-${item.foodCode}`">
                {{ item.foodName }} <b>{{ itemCalories(item) }}</b>
              </span>
                </div>
              </button>
            </article>

            <article class="record-timeline-row empty-slot">
              <div class="record-time">운동</div>
              <div class="record-line">
                <span :class="['record-icon', exerciseCount ? 'exercise' : 'disabled']">
                  <i class="pi pi-bolt"></i>
                </span>
              </div>
              <div v-if="exerciseCount" class="record-card">
                <div class="record-card-head">
                  <div>
                    <strong>운동</strong>
                    <span>{{ exerciseCount }}건</span>
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
                      <span>{{ exerciseMeta(record) }} · 클릭해서 수정</span>
                    </div>
                    <em>{{ formatNumber(record.caloriesBurned) }} kcal</em>
                  </button>
                </div>
              </div>
              <div v-else class="record-card muted-card">
                아직 운동 기록이 없습니다.
              </div>
            </article>
          </section>
        </template>

        <section v-else class="record-empty">
          <div class="record-empty-illustration">
            <i class="pi pi-comment"></i>
          </div>
          <h2>아직 기록이 없어요</h2>
          <p>오늘 먹은 식사나 운동을 코치에게 말해보세요. 캘린더가 차곡차곡 채워질 거예요.</p>
          <button type="button" @click="goToChat">코치와 대화 시작하기</button>
          <small>예: “아침에 그릭요거트를 먹었어”</small>
        </section>
      </section>
    </section>

    <div v-if="editForm.open" class="meal-edit-backdrop" @click.self="closeEditMeal">
      <section class="meal-edit-modal" role="dialog" aria-modal="true" aria-label="끼니 수정">
        <header>
          <div>
            <p class="deco">Edit Meal</p>
            <h2>{{ mealMeta(editForm.mealType).label }} 수정</h2>
          </div>
          <button type="button" aria-label="닫기" @click="closeEditMeal">
            <i class="pi pi-times"></i>
          </button>
        </header>

        <div class="meal-edit-search">
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

          <div v-else-if="mealStore.foodSearchResults.length" class="food-search-results">
            <button
              v-for="food in mealStore.foodSearchResults"
              :key="food.foodCode"
              type="button"
              @click="addFood(food)"
            >
              <strong>{{ food.foodName }}</strong>
              <span>{{ food.manufacturer || "제조사 정보 없음" }} · {{ servingLabel(food) }} · {{ formatNumber(food.calories) }} kcal</span>
            </button>
          </div>
        </div>

        <div class="meal-edit-items">
          <article v-for="item in editForm.items" :key="item.foodCode" class="meal-edit-item">
            <div>
              <strong>{{ item.foodName }}</strong>
              <span>{{ item.manufacturer || "제조사 정보 없음" }} · {{ servingCalorieLabel(item) }}</span>
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
            <button type="button" aria-label="음식 제거" @click="removeFood(item.foodCode)">
              <i class="pi pi-trash"></i>
            </button>
          </article>
        </div>

        <p v-if="mealStore.saveMealError || mealStore.deleteMealError" class="meal-edit-error">
          {{ mealStore.saveMealError || mealStore.deleteMealError }}
        </p>

        <footer>
          <button
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
              {{ mealStore.isSavingMeal ? "저장 중..." : "수정 저장" }}
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
            <h2>운동 수정</h2>
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
            <input v-model="exerciseEditForm.exerciseDate" type="date" />
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

        <p v-if="exerciseStore.saveRecordError" class="meal-edit-error">
          {{ exerciseStore.saveRecordError }}
        </p>

        <footer>
          <span></span>
          <div>
            <button class="meal-edit-cancel" type="button" @click="closeEditExercise">취소</button>
            <button
              class="meal-edit-save"
              type="button"
              :disabled="!canSaveExercise || exerciseStore.isSavingRecord"
              @click="saveEditedExercise"
            >
              {{ exerciseStore.isSavingRecord ? "저장 중..." : "수정 저장" }}
            </button>
          </div>
        </footer>
      </section>
    </div>
  </main>
</template>
