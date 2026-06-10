<script setup>
import { computed, ref } from "vue";
import AppSidebar from "../../components/app/AppSidebar.vue";

const foodQuery = ref("");
const selectedFoodId = ref(1);

const exampleQueries = ["닭가슴살", "요거트", "제육볶음", "감자칩"];

const mockFoods = [
  {
    id: 1,
    name: "닭가슴살",
    brand: "제조사 정보 없음",
    serving: "100g",
    calories: 165,
    carbohydrate: 0,
    protein: 31,
    fat: 3.6,
  },
  {
    id: 2,
    name: "매일 그릭요거트 플레인 무지방",
    brand: "매일유업",
    serving: "100g",
    calories: 162,
    carbohydrate: 12,
    protein: 17,
    fat: 0.3,
  },
  {
    id: 3,
    name: "제육볶음",
    brand: "일반 음식",
    serving: "1인분",
    calories: 354,
    carbohydrate: 18,
    protein: 31,
    fat: 19,
  },
  {
    id: 4,
    name: "감자칩",
    brand: "일반 음식",
    serving: "100g",
    calories: 536,
    carbohydrate: 53,
    protein: 7,
    fat: 35,
  },
  {
    id: 5,
    name: "아이스 아메리카노",
    brand: "폴바셋",
    serving: "1인분",
    calories: 8,
    carbohydrate: 1,
    protein: 0,
    fat: 0,
  },
  {
    id: 6,
    name: "바나나",
    brand: "일반 음식",
    serving: "100g",
    calories: 89,
    carbohydrate: 23,
    protein: 1.1,
    fat: 0.3,
  },
  {
    id: 7,
    name: "하이프로틴 초코",
    brand: "서울F&B",
    serving: "1인분",
    calories: 210,
    carbohydrate: 17,
    protein: 22,
    fat: 5,
  },
  {
    id: 8,
    name: "구운아몬드",
    brand: "롯데마트",
    serving: "100g",
    calories: 574,
    carbohydrate: 20,
    protein: 21,
    fat: 49,
  },
  {
    id: 9,
    name: "김치찌개",
    brand: "일반 음식",
    serving: "1인분",
    calories: 480,
    carbohydrate: 18,
    protein: 22,
    fat: 32,
  },
  {
    id: 10,
    name: "블루베리 생것",
    brand: "제조사 정보 없음",
    serving: "100g",
    calories: 57,
    carbohydrate: 14,
    protein: 0.7,
    fat: 0.3,
  },
];

const filteredFoods = computed(() => {
  const query = foodQuery.value.trim().toLowerCase();

  if (!query) {
    return mockFoods;
  }

  return mockFoods.filter((food) => {
    return `${food.name} ${food.brand}`.toLowerCase().includes(query);
  });
});

const selectedFood = computed(() => {
  return filteredFoods.value.find((food) => food.id === selectedFoodId.value) || filteredFoods.value[0] || null;
});

const macroTotal = computed(() => {
  if (!selectedFood.value) {
    return 0;
  }

  return selectedFood.value.carbohydrate + selectedFood.value.protein + selectedFood.value.fat;
});

function applyExampleQuery(query) {
  foodQuery.value = query;
  selectedFoodId.value = filteredFoods.value[0]?.id || selectedFoodId.value;
}

function selectFood(food) {
  selectedFoodId.value = food.id;
}

function formatNumber(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return "-";
  }

  return Number.isInteger(numberValue) ? numberValue.toLocaleString("ko-KR") : numberValue.toFixed(1);
}

function macroPercent(value) {
  if (!macroTotal.value) {
    return 0;
  }

  return Math.max(4, Math.round((value / macroTotal.value) * 100));
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
          총 49,148개 음식
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
              <h2>{{ filteredFoods.length }}개 결과</h2>
            </div>
            <div class="food-page-mock">
              <button type="button" aria-label="이전 페이지">
                <i class="pi pi-chevron-left"></i>
              </button>
              <span>1 / 1</span>
              <button type="button" aria-label="다음 페이지">
                <i class="pi pi-chevron-right"></i>
              </button>
            </div>
          </div>

          <div v-if="filteredFoods.length" class="food-result-list">
            <button
              v-for="food in filteredFoods"
              :key="food.id"
              type="button"
              class="food-result-card"
              :class="{ selected: selectedFood?.id === food.id }"
              @click="selectFood(food)"
            >
              <div>
                <strong>{{ food.name }}</strong>
                <span>{{ food.brand }} · {{ food.serving }} 기준</span>
              </div>
              <dl>
                <div>
                  <dt>칼로리</dt>
                  <dd>{{ formatNumber(food.calories) }} kcal</dd>
                </div>
                <div>
                  <dt>탄수화물</dt>
                  <dd>{{ formatNumber(food.carbohydrate) }}g</dd>
                </div>
                <div>
                  <dt>단백질</dt>
                  <dd>{{ formatNumber(food.protein) }}g</dd>
                </div>
                <div>
                  <dt>지방</dt>
                  <dd>{{ formatNumber(food.fat) }}g</dd>
                </div>
              </dl>
            </button>
          </div>

          <div v-else class="food-empty-state">
            <i class="pi pi-search"></i>
            <strong>검색 결과가 없어요</strong>
            <span>다른 음식명이나 제조사명으로 다시 검색해보세요.</span>
          </div>
        </section>

        <aside class="food-detail-panel" v-if="selectedFood">
          <p class="deco">Nutrition Preview</p>
          <h2>{{ selectedFood.name }}</h2>
          <span>{{ selectedFood.brand }} · {{ selectedFood.serving }} 기준</span>

          <div class="food-calorie-preview">
            <small>기준량당 칼로리</small>
            <strong>{{ formatNumber(selectedFood.calories) }}<em>kcal</em></strong>
          </div>

          <div class="food-macro-preview">
            <div>
              <span>탄수화물</span>
              <strong>{{ formatNumber(selectedFood.carbohydrate) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedFood.carbohydrate)}%` }"></i>
            </div>
            <div>
              <span>단백질</span>
              <strong>{{ formatNumber(selectedFood.protein) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedFood.protein)}%` }"></i>
            </div>
            <div>
              <span>지방</span>
              <strong>{{ formatNumber(selectedFood.fat) }}g</strong>
              <i :style="{ width: `${macroPercent(selectedFood.fat)}%` }"></i>
            </div>
          </div>

          <div class="food-detail-note">
            <strong>조회 전용 프로토타입</strong>
            <p>현재 화면은 API 연결 전 하드코딩 데이터로 구성되어 있어요. 다음 단계에서 페이지네이션 검색 API로 교체됩니다.</p>
          </div>
        </aside>
      </div>
    </section>
  </main>
</template>
