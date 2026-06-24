<script setup>
import { computed, onBeforeUnmount, reactive, watch } from "vue";
import { searchFoods } from "../../api/mealApi";
import { useAuthStore } from "../../stores/authStore";

const props = defineProps({
  proposal: {
    type: Object,
    required: true,
  },
  isConfirming: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["confirm", "dismiss"]);
const authStore = useAuthStore();

const itemStates = reactive({});
const searchTimers = new Map();
const searchSequences = reactive({});

const mealTypeLabels = {
  BREAKFAST: "아침",
  LUNCH: "점심",
  DINNER: "저녁",
  SNACK: "간식",
};

const mealLabel = computed(() => {
  return mealTypeLabels[props.proposal.mealType] || props.proposal.mealType;
});

const canConfirm = computed(() => {
  return props.proposal.items.every((_, index) => {
    const state = itemStates[index];
    return state?.foodId && Number(state.quantity) > 0 && !state.isSearching;
  });
});

watch(
  () => props.proposal,
  (proposal) => {
    resetSearchTimers();
    Object.keys(itemStates).forEach((key) => {
      delete itemStates[key];
      delete searchSequences[key];
    });

    proposal.items.forEach((item, index) => {
      const candidates = item.candidates || [];
      const firstCandidate = candidates[0];

      itemStates[index] = {
        editableName: item.extractedName || "",
        candidates,
        foodId: firstCandidate?.foodId || "",
        quantity: roundQuantity(item.quantity || 1),
        isSearching: false,
        searchError: "",
      };
      searchSequences[index] = 0;
    });
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  resetSearchTimers();
});

function selectCandidate(index, foodId) {
  itemStates[index].foodId = foodId;
}

function updateQuantity(index, value) {
  itemStates[index].quantity = roundQuantity(value);
}

function updateEditableName(index, value) {
  const state = itemStates[index];
  state.editableName = value;
  state.foodId = "";
  state.searchError = "";

  const query = value.trim();
  if (!query) {
    state.candidates = [];
    state.isSearching = false;
    clearSearchTimer(index);
    return;
  }

  state.isSearching = true;
  clearSearchTimer(index);
  searchTimers.set(
    index,
    window.setTimeout(() => {
      void searchCandidates(index, query);
    }, 250)
  );
}

async function searchCandidates(index, query) {
  const state = itemStates[index];
  if (!state) {
    return;
  }

  const sequence = (searchSequences[index] || 0) + 1;
  searchSequences[index] = sequence;

  try {
    const candidates = await searchFoods(authStore.accessToken, query);
    if (searchSequences[index] !== sequence) {
      return;
    }

    state.candidates = candidates;
    state.foodId = candidates[0]?.foodId || "";
    state.searchError = "";
  } catch (error) {
    if (authStore.handleAuthFailure(error)) {
      return;
    }

    if (searchSequences[index] === sequence) {
      state.candidates = [];
      state.foodId = "";
      state.searchError = error.message;
    }
  } finally {
    if (searchSequences[index] === sequence) {
      state.isSearching = false;
    }
  }
}

function clearSearchTimer(index) {
  const timer = searchTimers.get(index);
  if (timer) {
    window.clearTimeout(timer);
    searchTimers.delete(index);
  }
}

function resetSearchTimers() {
  searchTimers.forEach((timer) => window.clearTimeout(timer));
  searchTimers.clear();
}

function roundQuantity(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return 0.1;
  }

  return Math.round(numberValue * 10) / 10;
}

function selectedCandidate(index) {
  const state = itemStates[index];
  return state?.candidates?.find((candidate) => candidate.foodId === state.foodId);
}

function formatNumber(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return "-";
  }

  return Math.round(numberValue).toLocaleString("ko-KR");
}

function servingLabel(candidate) {
  const size = candidate.servingSize || 1;
  const unit = candidate.servingUnit || "회";

  return `${size}${unit} 기준`;
}

function confirm() {
  emit("confirm", {
    mealDate: props.proposal.mealDate,
    mealType: props.proposal.mealType,
    items: props.proposal.items.map((_, index) => ({
      foodId: itemStates[index].foodId,
      quantity: Number(itemStates[index].quantity),
    })),
  });
}
</script>

<template>
  <article class="proposal-card meal-proposal-card">
    <header class="proposal-head">
      <div>
        <p class="proposal-eyebrow">Meal Proposal</p>
        <h2 class="proposal-title">{{ mealLabel }} 식단으로 기록할까요?</h2>
      </div>
      <button class="proposal-close" type="button" aria-label="후보 닫기" @click="emit('dismiss')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <p class="proposal-desc">
      {{ proposal.mealDate }} · 음식 후보를 확인하고 실제로 먹은 배수를 조정해 주세요.
    </p>

    <div v-if="proposal.defaultsApplied?.length" class="proposal-badges">
      <span v-for="defaultText in proposal.defaultsApplied" :key="defaultText">{{ defaultText }}</span>
    </div>

    <div class="proposal-items">
      <section v-for="(item, index) in proposal.items" :key="`${item.extractedName}-${index}`">
        <div class="proposal-fields meal-proposal-fields">
          <label class="proposal-field">
            <span class="proposal-label">음식명</span>
            <input
              class="proposal-input"
              :value="itemStates[index]?.editableName"
              type="text"
              placeholder="음식명을 입력하세요"
              @input="updateEditableName(index, $event.target.value)"
            />
          </label>
          <label class="proposal-field proposal-field--narrow">
            <span class="proposal-label">배수</span>
            <input
              class="proposal-input"
              :value="itemStates[index]?.quantity"
              type="number"
              min="0.1"
              step="0.1"
              @input="updateQuantity(index, $event.target.value)"
            />
          </label>
        </div>

        <p v-if="itemStates[index]?.isSearching" class="proposal-status">검색 중...</p>
        <p v-else-if="itemStates[index]?.searchError" class="proposal-empty">
          {{ itemStates[index].searchError }}
        </p>

        <div v-if="itemStates[index]?.candidates?.length" class="proposal-options meal-candidate-list">
          <button
            v-for="candidate in itemStates[index].candidates"
            :key="candidate.foodId"
            type="button"
            :class="{ 'is-selected': itemStates[index]?.foodId === candidate.foodId }"
            @click="selectCandidate(index, candidate.foodId)"
          >
            <span class="proposal-option-name">{{ candidate.foodName }}</span>
            <span class="proposal-option-meta">
              {{ candidate.brand || "제조사 정보 없음" }} · {{ servingLabel(candidate) }} ·
              {{ formatNumber(candidate.calories) }} kcal
            </span>
          </button>
        </div>

        <div
          v-else-if="!itemStates[index]?.isSearching && !itemStates[index]?.searchError"
          class="proposal-empty"
        >
          매칭되는 음식 후보가 없어요. 이름을 다시 입력해주세요.
        </div>

        <p v-if="selectedCandidate(index)" class="proposal-selected-summary">
          선택됨: {{ selectedCandidate(index).foodName }}
        </p>
      </section>
    </div>

    <p v-if="error" class="proposal-error">{{ error }}</p>

    <footer class="proposal-actions">
      <button class="proposal-btn proposal-btn--ghost" type="button" @click="emit('dismiss')">나중에</button>
      <button class="proposal-btn proposal-btn--primary" type="button" :disabled="!canConfirm || isConfirming" @click="confirm">
        {{ isConfirming ? "기록 중..." : "식단 기록하기" }}
      </button>
    </footer>
  </article>
</template>
