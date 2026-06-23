<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import AppSidebar from "../../components/app/AppSidebar.vue";
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
const submissionOpen = ref(false);
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
  return foods.value.find((food) => food.sourceKey === selectedSourceKey.value) || foods.value[0] || null;
});

const selectedServing = computed(() => {
  if (!selectedFood.value) {
    return null;
  }

  return selectedFood.value.servings.find((serving) => serving.foodId === selectedServingId.value)
    || selectedFood.value.servings[0]
    || null;
});

const macroTotal = computed(() => {
  if (!selectedServing.value) {
    return 0;
  }

  return toNumber(selectedServing.value.carbohydrate)
    + toNumber(selectedServing.value.protein)
    + toNumber(selectedServing.value.fat);
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

  if (!newFoods.some((food) => food.sourceKey === selectedSourceKey.value)) {
    selectFood(newFoods[0]);
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
  foodQuery.value = query;
}

function selectFood(food) {
  selectedSourceKey.value = food.sourceKey;
  selectedServingId.value = food.servings?.[0]?.foodId || null;
  quantity.value = 1;
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

  const created = await foodStore.submitMissingFood({
    name: submissionForm.name.trim(),
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

function macroPercent(value) {
  if (!macroTotal.value) {
    return 0;
  }

  return Math.max(4, Math.round((toNumber(value) / macroTotal.value) * 100));
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
  <main class="food-home">
    <AppSidebar />

    <section class="food-workspace">
      <header class="food-header">
        <div>
          <p class="deco">Food Database</p>
          <h1>음식 검색</h1>
        </div>
        <div class="food-count-chip">
          <i></i>
          총 {{ formatNumber(foodStore.totalItems) }}개 음식
        </div>
      </header>

      <div class="food-content">
        <section class="food-main-panel">
          <div class="food-search-card">
            <label for="food-search-input">음식명이나 제조사명을 입력하세요</label>
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
              <p class="deco">Search Results</p>
              <h2>
                <template v-if="foodStore.isLoading">검색 중</template>
                <template v-else>{{ foods.length }}개 결과</template>
              </h2>
            </div>
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
              <div>
                <strong>{{ food.foodName }}</strong>
                <span>
                  {{ formatBrand(food.brand) }} · 대표 기준 {{ servingLabel(representativeServing(food)) }}
                </span>
                <em>{{ food.servings.length }}개 기준 선택 가능</em>
              </div>
              <dl>
                <div>
                  <dt>칼로리</dt>
                  <dd>{{ formatNumber(representativeServing(food)?.calories) }} kcal</dd>
                </div>
                <div>
                  <dt>탄수화물</dt>
                  <dd>{{ formatNumber(representativeServing(food)?.carbohydrate) }}g</dd>
                </div>
                <div>
                  <dt>단백질</dt>
                  <dd>{{ formatNumber(representativeServing(food)?.protein) }}g</dd>
                </div>
                <div>
                  <dt>지방</dt>
                  <dd>{{ formatNumber(representativeServing(food)?.fat) }}g</dd>
                </div>
              </dl>
            </button>
          </div>

          <div v-else class="food-empty-state">
            <i class="pi pi-search"></i>
            <strong>검색 결과가 없어요</strong>
            <span>다른 음식명이나 제조사명으로 다시 검색해보세요.</span>
          </div>

          <section class="food-submission-panel">
            <div class="food-submission-head">
              <div>
                <p class="deco">Missing Food</p>
                <h2>찾는 음식이 없나요?</h2>
                <span>음식 정보를 입력해 등록 요청을 보내면 관리자가 검토합니다.</span>
              </div>
              <button type="button" @click="submissionOpen = !submissionOpen">
                {{ submissionOpen ? "닫기" : "등록 요청" }}
              </button>
            </div>

            <form v-if="submissionOpen" class="food-submission-form" @submit.prevent="submitMissingFood">
              <label>
                <span>음식명</span>
                <input v-model="submissionForm.name" type="text" required />
              </label>
              <label>
                <span>브랜드</span>
                <input v-model="submissionForm.brand" type="text" />
              </label>
              <label>
                <span>기준 설명</span>
                <input v-model="submissionForm.servingDescription" type="text" placeholder="예: 100g, 1인분" />
              </label>
              <label>
                <span>기준 수치</span>
                <input v-model="submissionForm.servingSize" type="number" min="0" step="0.01" />
              </label>
              <label>
                <span>기준 단위</span>
                <input v-model="submissionForm.servingUnit" type="text" placeholder="g, ml, 인분" />
              </label>
              <label>
                <span>칼로리</span>
                <input v-model="submissionForm.calories" type="number" min="0" step="0.01" required />
              </label>
              <label>
                <span>탄수화물</span>
                <input v-model="submissionForm.carbohydrate" type="number" min="0" step="0.01" required />
              </label>
              <label>
                <span>단백질</span>
                <input v-model="submissionForm.protein" type="number" min="0" step="0.01" required />
              </label>
              <label>
                <span>지방</span>
                <input v-model="submissionForm.fat" type="number" min="0" step="0.01" required />
              </label>

              <button type="submit" :disabled="!canSubmitMissingFood || foodStore.isSubmittingFood">
                {{ foodStore.isSubmittingFood ? "요청 중" : "관리자에게 요청" }}
              </button>
            </form>

            <small v-if="foodStore.submissionMessage" class="food-meal-record-success">
              {{ foodStore.submissionMessage }}
            </small>
            <small v-if="foodStore.submissionError" class="food-meal-record-error">
              {{ foodStore.submissionError }}
            </small>

            <div v-if="myRequests.length" class="food-submission-history">
              <strong>내 최근 요청</strong>
              <span v-for="request in myRequests.slice(0, 3)" :key="request.id">
                {{ request.name }} · {{ statusLabel(request.status) }}
              </span>
            </div>
          </section>
        </section>

        <aside class="food-detail-panel" v-if="selectedFood && selectedServing">
          <p class="deco">Nutrition Preview</p>
          <h2>{{ selectedFood.foodName }}</h2>
          <span>{{ formatBrand(selectedFood.brand) }}</span>

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

          <div class="food-calorie-preview">
            <small>{{ servingLabel(selectedServing) }} 기준 칼로리</small>
            <strong>{{ formatNumber(selectedServing.calories) }}<em>kcal</em></strong>
          </div>

          <div class="food-macro-preview">
            <div>
              <span>탄수화물</span>
              <strong>{{ formatNumber(selectedServing.carbohydrate) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedServing.carbohydrate)}%` }"></i>
            </div>
            <div>
              <span>단백질</span>
              <strong>{{ formatNumber(selectedServing.protein) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedServing.protein)}%` }"></i>
            </div>
            <div>
              <span>지방</span>
              <strong>{{ formatNumber(selectedServing.fat) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedServing.fat)}%` }"></i>
            </div>
          </div>

          <div class="food-meal-record-panel">
            <strong>오늘 식단으로 기록</strong>
            <p>선택한 기준량을 오늘 식단에 바로 추가합니다. 같은 끼니의 기존 음식은 유지됩니다.</p>

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

            <button type="button" :disabled="!canSaveMeal" @click="saveSelectedFoodToToday">
              <i class="pi pi-plus"></i>
              {{ mealStore.isSavingMeal ? "기록 중" : "식단에 기록" }}
            </button>

            <small v-if="saveMessage" class="food-meal-record-success">{{ saveMessage }}</small>
            <small v-if="mealStore.saveMealError" class="food-meal-record-error">{{ mealStore.saveMealError }}</small>
          </div>
        </aside>
      </div>
    </section>
  </main>
</template>
