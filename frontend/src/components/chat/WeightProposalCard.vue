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

const form = reactive({
  recordDate: "",
  weightKg: "",
});
const todayDateKey = toDateKey(new Date());

const canConfirm = computed(() => {
  const weight = Number(form.weightKg);

  return (
    /^\d{4}-\d{2}-\d{2}$/.test(form.recordDate) &&
    form.recordDate <= todayDateKey &&
    Number.isFinite(weight) &&
    weight > 0 &&
    weight <= 500
  );
});

watch(
  () => props.proposal,
  (proposal) => {
    form.recordDate = proposal.recordDate || toDateKey(new Date());
    form.weightKg = proposal.weightKg ?? "";
  },
  { immediate: true }
);

function confirm() {
  if (!canConfirm.value) {
    return;
  }

  emit("confirm", {
    recordDate: form.recordDate,
    weightKg: Number(form.weightKg),
  });
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
</script>

<template>
  <article class="proposal-card weight-proposal-card">
    <header class="proposal-head">
      <div>
        <p class="proposal-eyebrow">Weight Proposal</p>
        <h2 class="proposal-title">몸무게 기록으로 저장할까요?</h2>
      </div>
      <button class="proposal-close" type="button" aria-label="후보 닫기" @click="emit('dismiss')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <p class="proposal-desc">
      대화에서 몸무게 {{ proposal.weightKg }}kg을 찾았어요. 날짜와 수치를 확인해 주세요.
    </p>

    <div class="proposal-fields weight-proposal-fields">
      <label class="proposal-field">
        <span class="proposal-label">기록 날짜</span>
        <input class="proposal-input" v-model="form.recordDate" :max="todayDateKey" type="date" />
      </label>

      <label class="proposal-field">
        <span class="proposal-label">몸무게(kg)</span>
        <input class="proposal-input" v-model="form.weightKg" type="number" min="0.1" max="500" step="any" />
      </label>
    </div>

    <p v-if="error" class="proposal-error">{{ error }}</p>

    <footer class="proposal-actions">
      <button class="proposal-btn proposal-btn--ghost" type="button" @click="emit('dismiss')">나중에</button>
      <button class="proposal-btn proposal-btn--primary" type="button" :disabled="!canConfirm || isConfirming" @click="confirm">
        {{ isConfirming ? "기록 중..." : "몸무게 기록하기" }}
      </button>
    </footer>
  </article>
</template>
