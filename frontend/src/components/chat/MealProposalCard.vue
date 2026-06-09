<script setup>
import { computed, reactive, watch } from "vue";

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

const selections = reactive({});

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
  return props.proposal.items.every((item, index) => {
    const selection = selections[index];
    return selection?.foodId && Number(selection.quantity) > 0;
  });
});

watch(
  () => props.proposal,
  (proposal) => {
    Object.keys(selections).forEach((key) => {
      delete selections[key];
    });

    proposal.items.forEach((item, index) => {
      const firstCandidate = item.candidates?.[0];

      selections[index] = {
        foodId: firstCandidate?.foodId || "",
        quantity: roundQuantity(item.quantity || 1),
      };
    });
  },
  { immediate: true }
);

function selectCandidate(index, foodId) {
  selections[index].foodId = foodId;
}

function updateQuantity(index, value) {
  selections[index].quantity = roundQuantity(value);
}

function roundQuantity(value) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return 0.1;
  }

  return Math.round(numberValue * 10) / 10;
}

function selectedCandidate(item, index) {
  return item.candidates?.find((candidate) => candidate.foodId === selections[index]?.foodId);
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
    items: props.proposal.items.map((item, index) => ({
      foodId: selections[index].foodId,
      quantity: Number(selections[index].quantity),
    })),
  });
}
</script>

<template>
  <article class="meal-proposal-card">
    <header>
      <div>
        <p class="deco">Meal Proposal</p>
        <h2>{{ mealLabel }} 식단으로 기록할까요?</h2>
      </div>
      <button type="button" aria-label="후보 닫기" @click="emit('dismiss')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <p class="meal-proposal-meta">
      {{ proposal.mealDate }} · 음식 후보를 확인하고 실제로 먹은 배수를 조정해 주세요.
    </p>

    <div v-if="proposal.defaultsApplied?.length" class="meal-proposal-defaults">
      <span v-for="defaultText in proposal.defaultsApplied" :key="defaultText">{{ defaultText }}</span>
    </div>

    <div class="meal-proposal-items">
      <section v-for="(item, index) in proposal.items" :key="`${item.extractedName}-${index}`">
        <div class="meal-proposal-item-head">
          <strong>{{ item.extractedName }}</strong>
          <label>
            <span>배수</span>
            <input
              :value="selections[index]?.quantity"
              type="number"
              min="0.1"
              step="0.1"
              @input="updateQuantity(index, $event.target.value)"
            />
          </label>
        </div>

        <div v-if="item.candidates?.length" class="meal-candidate-list">
          <button
            v-for="candidate in item.candidates"
            :key="candidate.foodId"
            type="button"
            :class="{ selected: selections[index]?.foodId === candidate.foodId }"
            @click="selectCandidate(index, candidate.foodId)"
          >
            <strong>{{ candidate.foodName }}</strong>
            <span>
              {{ candidate.brand || "제조사 정보 없음" }} · {{ servingLabel(candidate) }} · {{ formatNumber(candidate.calories) }} kcal
            </span>
          </button>
        </div>

        <div v-else class="meal-candidate-empty">
          매칭된 음식 후보가 없어서 기록할 수 없어요.
        </div>

        <p v-if="selectedCandidate(item, index)" class="meal-selected-summary">
          선택됨: {{ selectedCandidate(item, index).foodName }}
        </p>
      </section>
    </div>

    <p v-if="error" class="meal-proposal-error">{{ error }}</p>

    <footer>
      <button class="meal-proposal-cancel" type="button" @click="emit('dismiss')">나중에</button>
      <button class="meal-proposal-confirm" type="button" :disabled="!canConfirm || isConfirming" @click="confirm">
        {{ isConfirming ? "기록 중..." : "식단 기록하기" }}
      </button>
    </footer>
  </article>
</template>
