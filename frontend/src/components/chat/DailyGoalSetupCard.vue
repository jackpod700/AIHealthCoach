<script setup>
import { computed, reactive, watch } from "vue";
import { goalOptions } from "../../constants/authOptions";

const props = defineProps({
  initialGoalType: {
    type: String,
    default: "WEIGHT_LOSS",
  },
  recommendation: {
    type: Object,
    default: null,
  },
  isLoadingRecommendation: {
    type: Boolean,
    default: false,
  },
  isSaving: {
    type: Boolean,
    default: false,
  },
  recommendationError: {
    type: String,
    default: "",
  },
  saveError: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["recommend", "save"]);

const form = reactive({
  goalType: "WEIGHT_LOSS",
  calorieIntakeGoal: 1800,
  exerciseCalorieGoal: 300,
});

const recommendationBase = reactive({
  calorieIntakeGoal: 1800,
  exerciseCalorieGoal: 300,
});

const calorieRange = computed(() => {
  const base = recommendationBase.calorieIntakeGoal;

  return {
    min: Math.max(1000, roundToStep(base - 600, 50)),
    max: roundToStep(base + 600, 50),
    recommendedMin: Math.max(1000, roundToStep(base - 150, 50)),
    recommendedMax: roundToStep(base + 150, 50),
  };
});

const exerciseRange = computed(() => {
  const base = recommendationBase.exerciseCalorieGoal;

  return {
    min: Math.max(0, roundToStep(base - 200, 25)),
    max: roundToStep(base + 300, 25),
    recommendedMin: Math.max(0, roundToStep(base - 75, 25)),
    recommendedMax: roundToStep(base + 75, 25),
  };
});

const calorieBandStyle = computed(() => rangeBandStyle(calorieRange.value));

const exerciseBandStyle = computed(() => rangeBandStyle(exerciseRange.value));

const warnings = computed(() => {
  const result = [];

  if (form.calorieIntakeGoal < 1200) {
    result.push("너무 적은 섭취는 오히려 건강한 감량에 방해가 될 수 있어요.");
  }

  if (form.goalType === "WEIGHT_LOSS" && form.calorieIntakeGoal > 2500) {
    result.push("설정한 섭취 목표가 높으면 감량 속도가 느려질 수 있어요.");
  }

  if (form.exerciseCalorieGoal > 1000) {
    result.push(
      "운동 목표가 너무 높아요. 무리하지 않도록 컨디션에 맞게 조절해 주세요.",
    );
  }

  return result;
});

const hasImpossibleValues = computed(() => {
  return form.calorieIntakeGoal <= 0 || form.exerciseCalorieGoal < 0;
});

const calorieTone = computed(() => {
  if (form.calorieIntakeGoal < 1200) {
    return "low";
  }

  if (form.goalType === "WEIGHT_LOSS" && form.calorieIntakeGoal > 2500) {
    return "loose";
  }

  return "balanced";
});

const exerciseTone = computed(() => {
  return form.exerciseCalorieGoal > 1000 ? "high" : "balanced";
});

watch(
  () => props.initialGoalType,
  (goalType) => {
    if (goalType) {
      form.goalType = goalType;
    }
  },
  { immediate: true },
);

watch(
  () => props.recommendation,
  (recommendation) => {
    if (!recommendation) {
      return;
    }

    form.calorieIntakeGoal = Number(recommendation.calorieIntakeGoal);
    form.exerciseCalorieGoal = Number(recommendation.exerciseCalorieGoal);
    recommendationBase.calorieIntakeGoal = Number(
      recommendation.calorieIntakeGoal,
    );
    recommendationBase.exerciseCalorieGoal = Number(
      recommendation.exerciseCalorieGoal,
    );
  },
);

function selectGoal(goalType) {
  form.goalType = goalType;
  emit("recommend", goalType);
}

function saveGoal() {
  if (hasImpossibleValues.value || props.isSaving) {
    return;
  }

  emit("save", {
    goalType: form.goalType,
    calorieIntakeGoal: Number(form.calorieIntakeGoal),
    exerciseCalorieGoal: Number(form.exerciseCalorieGoal),
  });
}

function formatNumber(value) {
  return Math.round(Number(value) || 0).toLocaleString("ko-KR");
}

function roundToStep(value, step) {
  return Math.round(value / step) * step;
}

function rangeBandStyle(range) {
  const size = Math.max(range.max - range.min, 1);
  const left = ((range.recommendedMin - range.min) / size) * 100;
  const right = 100 - ((range.recommendedMax - range.min) / size) * 100;

  return {
    "--band-left": `${Math.max(0, Math.min(100, left))}%`,
    "--band-right": `${Math.max(0, Math.min(100, right))}%`,
  };
}
</script>

<template>
  <article class="daily-goal-setup-card">
    <div class="analysis-title">
      <i></i>
      <strong>목표 설정</strong>
    </div>

    <h2>우리 목표를 설정해볼까요?</h2>
    <p>목표 코스를 고르면 하루 섭취량과 운동량 추천값을 먼저 잡아드릴게요.</p>

    <div class="goal-setup-options">
      <button
        v-for="goal in goalOptions"
        :key="goal.value"
        type="button"
        :class="{ active: form.goalType === goal.value }"
        @click="selectGoal(goal.value)"
      >
        <i :class="goal.icon"></i>
        <span>{{ goal.title }}</span>
      </button>
    </div>

    <div v-if="recommendationError" class="daily-goal-inline-error">
      {{ recommendationError }}
    </div>

    <div class="goal-slider-list">
      <label :class="['goal-slider-field', calorieTone]">
        <span>
          <b>하루 섭취 목표</b>
          <strong>{{ formatNumber(form.calorieIntakeGoal) }} kcal</strong>
        </span>
        <div class="goal-range-wrap">
          <input
            v-model.number="form.calorieIntakeGoal"
            :min="calorieRange.min"
            :max="calorieRange.max"
            :style="calorieBandStyle"
            step="50"
            type="range"
          />
          <div class="goal-range-labels">
            <small>{{ formatNumber(calorieRange.min) }}</small>
            <small
              >추천 {{ formatNumber(calorieRange.recommendedMin) }}~{{
                formatNumber(calorieRange.recommendedMax)
              }}</small
            >
            <small>{{ formatNumber(calorieRange.max) }}</small>
          </div>
        </div>
        <input
          v-model.number="form.calorieIntakeGoal"
          min="1"
          step="10"
          type="number"
        />
      </label>

      <label :class="['goal-slider-field', exerciseTone]">
        <span>
          <b>하루 운동 목표</b>
          <strong>{{ formatNumber(form.exerciseCalorieGoal) }} kcal</strong>
        </span>
        <div class="goal-range-wrap">
          <input
            v-model.number="form.exerciseCalorieGoal"
            :min="exerciseRange.min"
            :max="exerciseRange.max"
            :style="exerciseBandStyle"
            step="25"
            type="range"
          />
          <div class="goal-range-labels">
            <small>{{ formatNumber(exerciseRange.min) }}</small>
            <small
              >추천 {{ formatNumber(exerciseRange.recommendedMin) }}~{{
                formatNumber(exerciseRange.recommendedMax)
              }}</small
            >
            <small>{{ formatNumber(exerciseRange.max) }}</small>
          </div>
        </div>
        <input
          v-model.number="form.exerciseCalorieGoal"
          min="0"
          step="10"
          type="number"
        />
      </label>
    </div>

    <div v-if="warnings.length" class="daily-goal-warning-list">
      <p v-for="warning in warnings" :key="warning">{{ warning }}</p>
    </div>

    <div v-if="saveError" class="daily-goal-inline-error">
      {{ saveError }}
    </div>

    <button
      class="daily-goal-save-button"
      type="button"
      :disabled="isLoadingRecommendation || isSaving || hasImpossibleValues"
      @click="saveGoal"
    >
      {{ isSaving ? "저장 중..." : "이 목표로 시작하기" }}
    </button>
  </article>
</template>
