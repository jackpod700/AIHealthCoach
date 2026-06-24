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
  exerciseActivityOptionId: null,
  intensityLevel: "MEDIUM",
  exerciseDate: "",
  durationMinutes: 30,
  memo: "",
});

const missingFieldLabels = {
  exerciseDate: "날짜",
  durationMinutes: "시간",
  intensityLevel: "강도",
};

const canConfirm = computed(() => {
  return Boolean(
    form.exerciseActivityOptionId &&
      ["LOW", "MEDIUM", "HIGH"].includes(form.intensityLevel) &&
      /^\d{4}-\d{2}-\d{2}$/.test(form.exerciseDate) &&
      Number(form.durationMinutes) > 0
  );
});

watch(
  () => props.proposal,
  (proposal) => {
    const firstCandidate = proposal.candidates?.[0];

    form.exerciseActivityOptionId = firstCandidate?.id || null;
    form.intensityLevel = proposal.intensityLevel || "MEDIUM";
    form.exerciseDate = proposal.exerciseDate || "";
    form.durationMinutes = Number(proposal.durationMinutes || 30);
    form.memo = proposal.memo || proposal.activityKeyword || "";
  },
  { immediate: true }
);

function selectCandidate(candidateId) {
  form.exerciseActivityOptionId = candidateId;
}

function selectIntensity(level) {
  form.intensityLevel = level;
}

function selectedCandidate() {
  return props.proposal.candidates?.find((candidate) => candidate.id === form.exerciseActivityOptionId);
}

function intensityLabel(level) {
  const labels = {
    LOW: "하",
    MEDIUM: "중",
    HIGH: "상",
  };

  return labels[level] || level;
}

function metLabel(candidate, level) {
  const intensity = candidate?.[level.toLowerCase()];
  const value = Number(intensity?.metValue);

  if (!Number.isFinite(value)) {
    return "";
  }

  return `${value.toLocaleString("ko-KR", { maximumFractionDigits: 1 })} MET`;
}

function candidateMeta(candidate) {
  const values = ["LOW", "MEDIUM", "HIGH"]
    .map((level) => metLabel(candidate, level))
    .filter(Boolean);

  return `${candidate.majorHeading || "운동 옵션"}${values.length ? ` · ${values.join(" / ")}` : ""}`;
}

function missingFieldText(field) {
  return missingFieldLabels[field] || field;
}

function confirm() {
  emit("confirm", {
    exerciseActivityOptionId: form.exerciseActivityOptionId,
    intensityLevel: form.intensityLevel,
    exerciseDate: form.exerciseDate,
    durationMinutes: Number(form.durationMinutes),
    memo: form.memo,
  });
}
</script>

<template>
  <article class="proposal-card exercise-proposal-card">
    <header class="proposal-head">
      <div>
        <p class="proposal-eyebrow">Exercise Proposal</p>
        <h2 class="proposal-title">운동 기록으로 저장할까요?</h2>
      </div>
      <button class="proposal-close" type="button" aria-label="후보 닫기" @click="emit('dismiss')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <p class="proposal-desc">
      {{ proposal.activityKeyword }} · 후보 운동과 강도, 시간을 확인해 주세요.
    </p>

    <div v-if="proposal.missingFields?.length" class="proposal-badges">
      <span v-for="field in proposal.missingFields" :key="field">{{ missingFieldText(field) }} 확인 필요</span>
    </div>

    <div v-if="proposal.candidates?.length" class="proposal-options exercise-candidate-list">
      <button
        v-for="candidate in proposal.candidates"
        :key="candidate.id"
        type="button"
        :class="{ 'is-selected': form.exerciseActivityOptionId === candidate.id }"
        @click="selectCandidate(candidate.id)"
      >
        <span class="proposal-option-name">{{ candidate.activityNameKo }}</span>
        <span class="proposal-option-meta">{{ candidateMeta(candidate) }}</span>
      </button>
    </div>

    <div v-else class="proposal-empty">
      매칭된 운동 후보가 없어서 기록할 수 없어요.
    </div>

    <div class="proposal-fields exercise-proposal-fields">
      <label class="proposal-field proposal-field--intensity">
        <span class="proposal-label">강도</span>
        <div class="proposal-intensity-options">
          <button
            v-for="level in ['LOW', 'MEDIUM', 'HIGH']"
            :key="level"
            type="button"
            :class="{ 'is-selected': form.intensityLevel === level }"
            @click="selectIntensity(level)"
          >
            {{ intensityLabel(level) }}
          </button>
        </div>
      </label>

      <label class="proposal-field">
        <span class="proposal-label">운동 날짜</span>
        <input class="proposal-input" v-model="form.exerciseDate" type="date" />
      </label>

      <label class="proposal-field proposal-field--narrow">
        <span class="proposal-label">운동 시간</span>
        <input class="proposal-input" v-model="form.durationMinutes" type="number" min="1" step="1" />
      </label>

      <label class="proposal-field exercise-proposal-memo">
        <span class="proposal-label">메모</span>
        <input class="proposal-input" v-model="form.memo" placeholder="메모를 입력하세요" />
      </label>
    </div>

    <p v-if="selectedCandidate()" class="proposal-selected-summary">
      선택됨: {{ selectedCandidate().activityNameKo }} · {{ intensityLabel(form.intensityLevel) }} · {{ metLabel(selectedCandidate(), form.intensityLevel) }}
    </p>

    <p v-if="error" class="proposal-error">{{ error }}</p>

    <footer class="proposal-actions">
      <button class="proposal-btn proposal-btn--ghost" type="button" @click="emit('dismiss')">나중에</button>
      <button class="proposal-btn proposal-btn--primary" type="button" :disabled="!canConfirm || isConfirming" @click="confirm">
        {{ isConfirming ? "기록 중..." : "운동 기록하기" }}
      </button>
    </footer>
  </article>
</template>
