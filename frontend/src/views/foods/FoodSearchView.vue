<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useFoodStore } from "../../stores/foodStore";
import { useMealStore } from "../../stores/mealStore";

const foodStore = useFoodStore();
const mealStore = useMealStore();
const FOOD_SEARCH_DEBOUNCE_MS = 300;
const SEARCH_MISS_RECORD_DELAY_MS = 700;

const foodQuery = ref("");
const selectedSourceKey = ref("");
const selectedServingId = ref(null);
const debounceTimer = ref(null);
const missRecordTimer = ref(null);
const mealType = ref(defaultMealType());
const quantity = ref(1);
const saveMessage = ref("");
const foodAddOpen = ref(false);
const submissionComplete = ref(false);
const completedSubmissionName = ref("");
const showNutritionPanel = ref(false);
const isNutritionPanelClosing = ref(false);
const submissionForm = reactive({
  name: "",
  brand: "",
  servingDescription: "",
  servingSize: "",
  servingUnit: "",
  calories: "",
  carbohydrate: "",
  protein: "",
  fat: "",
});

const exampleQueries = ["닭가슴살", "요거트", "제육볶음", "감자칩"];
const mealTypeOptions = [
  { value: "BREAKFAST", label: "아침" },
  { value: "LUNCH", label: "점심" },
  { value: "DINNER", label: "저녁" },
  { value: "SNACK", label: "간식" },
];

const foods = computed(() => foodStore.foods);
const myRequests = computed(() => foodStore.submissionPage?.items || []);

const selectedFood = computed(() => {
  return foods.value.find((food) => food.sourceKey === selectedSourceKey.value) || null;
});

const selectedServing = computed(() => {
  if (!selectedFood.value) {
    return null;
  }

  return selectedFood.value.servings.find((serving) => serving.foodId === selectedServingId.value)
    || selectedFood.value.servings[0]
    || null;
});

const canShowNutritionPanel = computed(() => {
  return showNutritionPanel.value && selectedFood.value && selectedServing.value;
});

const shouldKeepNutritionLayout = computed(() => {
  return canShowNutritionPanel.value || isNutritionPanelClosing.value;
});

const canGoPrevious = computed(() => foodStore.foodPage.page > 1);
const canGoNext = computed(() => foodStore.foodPage.page < foodStore.foodPage.totalPages);
const canSaveMeal = computed(() => {
  return Boolean(selectedServing.value?.foodId) && Number(quantity.value) > 0 && !mealStore.isSavingMeal;
});

const canSubmitMissingFood = computed(() => {
  return submissionForm.name.trim()
    && Number(submissionForm.calories) >= 0
    && Number(submissionForm.carbohydrate) >= 0
    && Number(submissionForm.protein) >= 0
    && Number(submissionForm.fat) >= 0
    && (
      submissionForm.servingDescription.trim()
      || (Number(submissionForm.servingSize) > 0 && submissionForm.servingUnit.trim())
    );
});

watch(foods, (newFoods) => {
  if (!newFoods.length) {
    selectedSourceKey.value = "";
    selectedServingId.value = null;
    return;
  }

  if (
    selectedSourceKey.value &&
    !newFoods.some((food) => food.sourceKey === selectedSourceKey.value)
  ) {
    closeNutritionPanel();
  }
});

watch(foodQuery, (query) => {
  clearDebounceTimer();
  clearMissRecordTimer();

  debounceTimer.value = window.setTimeout(async () => {
    const trimmedQuery = query.trim();
    await foodStore.loadFoodGroups({
      query: trimmedQuery,
      page: 1,
      size: foodStore.size,
    });

    if (foodQuery.value.trim() === trimmedQuery) {
      scheduleSearchMissRecord(trimmedQuery);
    }
  }, FOOD_SEARCH_DEBOUNCE_MS);
});

watch([selectedFood, selectedServing], () => {
  saveMessage.value = "";
});

onMounted(() => {
  foodStore.loadFoodGroups({ query: "", page: 1, size: foodStore.size });
  foodStore.loadMyFoodSubmissions();
});

onBeforeUnmount(() => {
  clearDebounceTimer();
  clearMissRecordTimer();
});

function representativeServing(food) {
  return food.servings?.[0] || null;
}

function applyExampleQuery(query) {
  foodAddOpen.value = false;
  foodQuery.value = query;
}

function openFoodAddForm() {
  resetSubmissionState();
  foodAddOpen.value = true;
}

function closeFoodAddForm() {
  foodAddOpen.value = false;
  resetSubmissionState();
}

function selectFood(food) {
  foodAddOpen.value = false;
  selectedSourceKey.value = food.sourceKey;
  selectedServingId.value = food.servings?.[0]?.foodId || null;
  quantity.value = 1;
  isNutritionPanelClosing.value = false;
  showNutritionPanel.value = true;
}

function closeNutritionPanel() {
  if (!showNutritionPanel.value) {
    clearNutritionPanelSelection();
    return;
  }

  isNutritionPanelClosing.value = true;
  showNutritionPanel.value = false;
}

function clearNutritionPanelSelection() {
  selectedSourceKey.value = "";
  selectedServingId.value = null;
  quantity.value = 1;
}

function afterNutritionPanelLeave() {
  clearNutritionPanelSelection();
  isNutritionPanelClosing.value = false;
}

function selectServing(serving) {
  selectedServingId.value = serving.foodId;
  quantity.value = 1;
}

function goToPage(page) {
  if (page < 1 || page > foodStore.foodPage.totalPages || foodStore.isLoading) {
    return;
  }

  foodStore.loadFoodGroups({
    query: foodQuery.value.trim(),
    page,
    size: foodStore.size,
  });
}

async function saveSelectedFoodToToday() {
  if (!canSaveMeal.value) {
    return;
  }

  const today = todayDateKey();
  saveMessage.value = "";
  await mealStore.loadDailyMeal(today);

  const existingMeal = mealStore.dailyMeal?.meals?.find((meal) => meal.mealType === mealType.value);
  const existingItems = existingMeal?.items || [];
  const selectedFoodId = selectedServing.value.foodId;
  const nextItems = existingItems
    .filter((item) => item.foodId !== selectedFoodId)
    .map((item) => ({
      foodId: item.foodId,
      quantity: item.quantity,
    }));

  nextItems.push({
    foodId: selectedFoodId,
    quantity: Number(quantity.value),
  });

  const saved = await mealStore.saveMealItems({
    mealDate: today,
    mealType: mealType.value,
    items: nextItems,
  });

  if (saved) {
    saveMessage.value = `${mealTypeLabel(mealType.value)} 식단에 기록했어요.`;
  }
}

async function submitMissingFood() {
  if (!canSubmitMissingFood.value) {
    return;
  }

  const submittedName = submissionForm.name.trim();
  const created = await foodStore.submitMissingFood({
    name: submittedName,
    brand: blankToNull(submissionForm.brand),
    servingDescription: blankToNull(submissionForm.servingDescription),
    servingSize: numberOrNull(submissionForm.servingSize),
    servingUnit: blankToNull(submissionForm.servingUnit),
    calories: Number(submissionForm.calories),
    carbohydrate: Number(submissionForm.carbohydrate),
    protein: Number(submissionForm.protein),
    fat: Number(submissionForm.fat),
  });

  if (created) {
    submissionComplete.value = true;
    completedSubmissionName.value = submittedName;
    Object.assign(submissionForm, {
      name: "",
      brand: "",
      servingDescription: "",
      servingSize: "",
      servingUnit: "",
      calories: "",
      carbohydrate: "",
      protein: "",
      fat: "",
    });
  }
}

function requestAnotherFood() {
  resetSubmissionState();
}

function servingLabel(serving) {
  if (!serving) {
    return "-";
  }

  return serving.servingDescription || [
    formatNumber(serving.servingSize),
    serving.servingUnit,
  ].filter(Boolean).join("");
}

function mealTypeLabel(value) {
  return mealTypeOptions.find((option) => option.value === value)?.label || value;
}

function statusLabel(status) {
  if (status === "APPROVED") {
    return "승인";
  }
  if (status === "REJECTED") {
    return "반려";
  }
  return "대기";
}

function statusClass(status) {
  if (status === "APPROVED") {
    return "approved";
  }
  if (status === "REJECTED") {
    return "rejected";
  }
  return "pending";
}

function formatBrand(brand) {
  return brand || "제조사 정보 없음";
}

function formatNumber(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return "-";
  }

  return Number.isInteger(numberValue) ? numberValue.toLocaleString("ko-KR") : numberValue.toFixed(1);
}

function toNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function defaultMealType() {
  const hour = new Date().getHours();

  if (hour < 10) {
    return "BREAKFAST";
  }
  if (hour < 15) {
    return "LUNCH";
  }
  if (hour < 21) {
    return "DINNER";
  }
  return "SNACK";
}

function todayDateKey() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function blankToNull(value) {
  const trimmed = String(value || "").trim();
  return trimmed || null;
}

function numberOrNull(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null;
}

function resetSubmissionState() {
  submissionComplete.value = false;
  completedSubmissionName.value = "";
  foodStore.clearSubmissionFeedback();
}

function scheduleSearchMissRecord(query) {
  clearMissRecordTimer();

  if (!shouldRecordSearchMiss(query) || foodStore.error || foods.value.length) {
    return;
  }

  missRecordTimer.value = window.setTimeout(() => {
    if (
      foodQuery.value.trim() !== query
      || foodStore.isLoading
      || foodStore.error
      || foods.value.length
    ) {
      return;
    }

    foodStore.recordSearchMiss(query);
  }, SEARCH_MISS_RECORD_DELAY_MS);
}

function shouldRecordSearchMiss(query) {
  const normalizedQuery = String(query || "").trim();
  const compactQuery = normalizedQuery.replace(/\s+/g, "");
  return compactQuery.length >= 2 && /[\p{L}\p{N}]/u.test(normalizedQuery);
}

function clearDebounceTimer() {
  if (debounceTimer.value) {
    window.clearTimeout(debounceTimer.value);
    debounceTimer.value = null;
  }
}

function clearMissRecordTimer() {
  if (missRecordTimer.value) {
    window.clearTimeout(missRecordTimer.value);
    missRecordTimer.value = null;
  }
}
</script>

<template>
      <header class="record-header food-record-header">
        <div>
          <p class="deco">Food Search</p>
          <h1>음식 검색</h1>
        </div>
        <div class="streak-chip">
          <i></i>
          총 {{ formatNumber(foodStore.totalItems) }}개 음식
        </div>
      </header>

      <div
        class="food-content"
        :class="{
          'has-nutrition-panel': shouldKeepNutritionLayout,
        }"
      >
        <section class="food-main-panel">
            <div class="food-search-card">
              <div class="food-search-input">
                <i class="pi pi-search"></i>
                <input
                  id="food-search-input"
                  v-model="foodQuery"
                  type="search"
                  placeholder="음식명이나 제조사명을 입력하세요"
                />
              </div>
              <div class="food-query-chips" aria-label="예시 검색어">
                <button v-for="query in exampleQueries" :key="query" type="button" @click="applyExampleQuery(query)">
                  {{ query }}
                </button>
              </div>
            </div>

            <div class="food-results-head">
              <div>
                <h2>
                  <template v-if="foodStore.isLoading">검색 중</template>
                  <template v-else>검색 결과 {{ formatNumber(foodStore.totalItems) }}개</template>
                </h2>
              </div>
              <div class="food-results-actions">
                <div class="food-page-mock">
                  <button type="button" aria-label="이전 페이지" :disabled="!canGoPrevious" @click="goToPage(foodStore.foodPage.page - 1)">
                    <i class="pi pi-chevron-left"></i>
                  </button>
                  <span>{{ foodStore.foodPage.page }} / {{ foodStore.foodPage.totalPages || 1 }}</span>
                  <button type="button" aria-label="다음 페이지" :disabled="!canGoNext" @click="goToPage(foodStore.foodPage.page + 1)">
                    <i class="pi pi-chevron-right"></i>
                  </button>
                </div>
              </div>
            </div>

            <div class="food-results-scroll">
              <div v-if="foodStore.error" class="food-empty-state">
                <i class="pi pi-exclamation-triangle"></i>
                <strong>음식 정보를 불러오지 못했어요</strong>
                <span>{{ foodStore.error }}</span>
              </div>

              <div v-else-if="foods.length" class="food-result-list">
                <button
                  v-for="food in foods"
                  :key="food.sourceKey"
                  type="button"
                  class="food-result-card"
                  :class="{ selected: selectedFood?.sourceKey === food.sourceKey }"
                  @click="selectFood(food)"
                >
                  <div class="food-result-name">
                    <strong>{{ food.foodName }}</strong>
                    <span>{{ food.servings.length }}개 기준</span>
                  </div>
                  <small>{{ formatBrand(food.brand) }} · {{ servingLabel(representativeServing(food)) }}</small>
                  <em>{{ formatNumber(representativeServing(food)?.calories) }} kcal</em>
                  <i class="pi pi-chevron-right"></i>
                </button>
              </div>

              <div v-else class="food-empty-state">
                <i class="pi pi-search"></i>
                <strong>검색 결과가 없어요</strong>
                <span>다른 음식명이나 제조사명으로 다시 검색해보세요.</span>
              </div>
            </div>

            <button
              type="button"
              class="food-add-fab"
              aria-label="찾는 음식 등록 요청 폼 열기"
              @click="openFoodAddForm"
            >
              <span>찾는 음식이 없나요?</span>
              <i class="pi pi-plus" aria-hidden="true"></i>
            </button>
        </section>

        <Transition name="food-detail-slide" @after-leave="afterNutritionPanelLeave">
        <aside class="food-detail-panel" v-if="canShowNutritionPanel">
          <header class="food-detail-head">
            <div>
              <p class="deco">Nutrition</p>
              <h2>{{ selectedFood.foodName }}</h2>
              <span>{{ formatBrand(selectedFood.brand) }}</span>
            </div>
            <button
              type="button"
              aria-label="영양 정보 닫기"
              @click="closeNutritionPanel"
            >
              <i class="pi pi-times"></i>
            </button>
          </header>

          <div class="food-detail-scroll">
            <div class="food-serving-switcher">
              <strong>기준 선택</strong>
              <p>같은 음식도 제공량 기준에 따라 영양성분이 달라집니다.</p>
              <div>
                <button
                  v-for="serving in selectedFood.servings"
                  :key="serving.foodId"
                  type="button"
                  :class="{ active: selectedServing.foodId === serving.foodId }"
                  @click="selectServing(serving)"
                >
                  <span>{{ servingLabel(serving) }}</span>
                  <b>{{ formatNumber(serving.calories) }} kcal</b>
                </button>
              </div>
            </div>

            <div class="food-macro-preview">
              <div>
                <span>탄수화물</span>
                <strong>{{ formatNumber(selectedServing.carbohydrate) }}g</strong>
              </div>
              <div>
                <span>단백질</span>
                <strong>{{ formatNumber(selectedServing.protein) }}g</strong>
              </div>
              <div>
                <span>지방</span>
                <strong>{{ formatNumber(selectedServing.fat) }}g</strong>
              </div>
            </div>
          </div>

          <div class="food-meal-record-panel">
            <strong>오늘 식단으로 기록</strong>
            <p>선택한 기준량을 오늘 식단에 바로 추가합니다. 같은 끼니의 기존 음식은 유지됩니다.</p>

            <div class="food-meal-record-fields">
              <label>
                <span>끼니</span>
                <select v-model="mealType">
                  <option v-for="option in mealTypeOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>배수</span>
                <input v-model.number="quantity" type="number" min="0.1" step="0.1" />
              </label>
            </div>

            <button type="button" :disabled="!canSaveMeal" @click="saveSelectedFoodToToday">
              <i class="pi pi-plus"></i>
              {{ mealStore.isSavingMeal ? "기록 중" : "식단에 기록" }}
            </button>

            <small v-if="saveMessage" class="food-meal-record-success">{{ saveMessage }}</small>
            <small v-if="mealStore.saveMealError" class="food-meal-record-error">{{ mealStore.saveMealError }}</small>
          </div>
        </aside>
        </Transition>

        <Transition name="food-submission-overlay">
          <div
            v-if="foodAddOpen"
            class="food-submission-overlay"
            @click.self="closeFoodAddForm"
          >
            <section class="food-submission-panel food-submission-panel-form">
              <div class="food-submission-head">
                <div>
                  <p class="deco">Missing Food</p>
                  <h2>찾는 음식이 없나요?</h2>
                  <span>음식 정보를 입력해 등록 요청을 보내면 관리자가 검토합니다.</span>
                </div>
                <button
                  type="button"
                  class="food-submission-close"
                  aria-label="음식 등록 요청 창 닫기"
                  @click="closeFoodAddForm"
                >
                  <i class="pi pi-times" aria-hidden="true"></i>
                </button>
              </div>

              <div v-if="submissionComplete" class="food-submission-complete" role="status" aria-live="polite">
                <div class="food-submission-complete-icon">
                  <i class="pi pi-check" aria-hidden="true"></i>
                </div>
                <div>
                  <strong>요청 완료</strong>
                  <p>
                    <b>{{ completedSubmissionName }}</b> 등록 요청을 보냈어요.
                    관리자가 확인한 뒤 음식 DB에 반영됩니다.
                  </p>
                </div>
                <div class="food-submission-complete-actions">
                  <button type="button" class="secondary" @click="requestAnotherFood">
                    다른 요청하기
                  </button>
                  <button type="button" @click="closeFoodAddForm">
                    닫기
                  </button>
                </div>
              </div>

              <form v-else class="food-submission-form" @submit.prevent="submitMissingFood">
                <label>
                  <span>음식명</span>
                  <input v-model="submissionForm.name" type="text" placeholder="예: 닭가슴살" required />
                </label>
                <label>
                  <span>브랜드</span>
                  <input v-model="submissionForm.brand" type="text" placeholder="제조사 또는 브랜드" />
                </label>
                <label>
                  <span>기준 설명</span>
                  <input v-model="submissionForm.servingDescription" type="text" placeholder="예: 100g 기준" />
                </label>
                <label>
                  <span>기준 수치</span>
                  <input v-model="submissionForm.servingSize" type="number" min="0" step="0.01" placeholder="100" />
                </label>
                <label>
                  <span>기준 단위</span>
                  <input v-model="submissionForm.servingUnit" type="text" placeholder="g" />
                </label>
                <label>
                  <span>칼로리</span>
                  <input v-model="submissionForm.calories" type="number" min="0" step="0.01" placeholder="kcal" required />
                </label>
                <label>
                  <span>탄수화물</span>
                  <input v-model="submissionForm.carbohydrate" type="number" min="0" step="0.01" placeholder="g" required />
                </label>
                <label>
                  <span>단백질</span>
                  <input v-model="submissionForm.protein" type="number" min="0" step="0.01" placeholder="g" required />
                </label>
                <label>
                  <span>지방</span>
                  <input v-model="submissionForm.fat" type="number" min="0" step="0.01" placeholder="g" required />
                </label>

                <button type="submit" :disabled="!canSubmitMissingFood || foodStore.isSubmittingFood">
                  <i class="pi pi-send" aria-hidden="true"></i>
                  {{ foodStore.isSubmittingFood ? "요청 중" : "관리자에게 요청" }}
                </button>
              </form>

              <small v-if="!submissionComplete && foodStore.submissionError" class="food-meal-record-error">
                {{ foodStore.submissionError }}
              </small>

              <div v-if="!submissionComplete && myRequests.length" class="food-submission-history">
                <strong>내 최근 요청</strong>
                <span v-for="request in myRequests.slice(0, 3)" :key="request.id" class="food-submission-history-item">
                  <b>{{ request.name }}</b>
                  <em :class="statusClass(request.status)">{{ statusLabel(request.status) }}</em>
                </span>
              </div>
            </section>
          </div>
        </Transition>
      </div>
</template>
