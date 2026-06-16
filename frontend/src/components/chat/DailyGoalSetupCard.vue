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
  title: {
    type: String,
    default: "우리 목표를 설정해볼까요?",
  },
  description: {
    type: String,
    default:
      "목표 코스를 고르면 하루 섭취량과 운동량 추천값을 먼저 잡아드릴게요.",
  },
  submitLabel: {
    type: String,
    default: "이 목표로 시작하기",
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

const GOAL_RANGE_CONFIG = {
  WEIGHT_LOSS: {
    calorie: {
      minOffset: -500,
      maxOffset: 400,
      recommendedMinOffset: -200,
      recommendedMaxOffset: 100,
      lowWarning:
        "섭취 목표가 너무 낮아요. 너무 적은 섭취는 건강한 감량에 방해가 될 수 있어요.",
      highWarning:
        "섭취 목표가 감량 구간보다 넉넉해요. 감량 속도가 느려질 수 있어요.",
    },
    exercise: {
      minOffset: -150,
      maxOffset: 350,
      recommendedMinOffset: -50,
      recommendedMaxOffset: 150,
      lowWarning:
        "운동 목표가 감량 추천 구간보다 낮아요. 활동량을 조금 더 확보해보세요.",
      highWarning:
        "운동 목표가 높은 편이에요. 회복을 고려해 무리하지 않게 조절해 주세요.",
    },
  },
  MAINTENANCE: {
    calorie: {
      minOffset: -500,
      maxOffset: 500,
      recommendedMinOffset: -150,
      recommendedMaxOffset: 150,
      lowWarning:
        "섭취 목표가 유지 구간보다 낮아요. 체중이 의도보다 줄 수 있어요.",
      highWarning:
        "섭취 목표가 유지 구간보다 높아요. 체중이 서서히 늘 수 있어요.",
    },
    exercise: {
      minOffset: -150,
      maxOffset: 300,
      recommendedMinOffset: -75,
      recommendedMaxOffset: 125,
      lowWarning:
        "운동 목표가 유지 추천 구간보다 낮아요. 컨디션 유지가 어려울 수 있어요.",
      highWarning:
        "운동 목표가 높은 편이에요. 피로가 쌓이지 않게 조절해 주세요.",
    },
  },
  MUSCLE_GAIN: {
    calorie: {
      minOffset: -400,
      maxOffset: 700,
      recommendedMinOffset: -100,
      recommendedMaxOffset: 250,
      lowWarning:
        "섭취 목표가 근성장 추천 구간보다 낮아요. 회복과 근육 증가에 부족할 수 있어요.",
      highWarning:
        "섭취 목표가 높은 편이에요. 체지방 증가 속도가 빨라질 수 있어요.",
    },
    exercise: {
      minOffset: -100,
      maxOffset: 400,
      recommendedMinOffset: -50,
      recommendedMaxOffset: 200,
      lowWarning:
        "운동 목표가 근성장 추천 구간보다 낮아요. 충분한 운동 자극을 확보해보세요.",
      highWarning:
        "운동 목표가 높은 편이에요. 근성장에는 휴식과 회복도 중요해요.",
    },
  },
};

const activeRangeConfig = computed(() => {
  return GOAL_RANGE_CONFIG[form.goalType] || GOAL_RANGE_CONFIG.WEIGHT_LOSS;
});

const calorieRange = computed(() => {
  const base = recommendationBase.calorieIntakeGoal;
  const config = activeRangeConfig.value.calorie;

  return {
    min: Math.max(1000, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
    recommendedMin: Math.max(
      1000,
      roundToStep(base + config.recommendedMinOffset, 50),
    ),
    recommendedMax: roundToStep(base + config.recommendedMaxOffset, 50),
  };
});

const exerciseRange = computed(() => {
  const base = recommendationBase.exerciseCalorieGoal;
  const config = activeRangeConfig.value.exercise;

  return {
    min: Math.max(0, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
    recommendedMin: Math.max(
      0,
      roundToStep(base + config.recommendedMinOffset, 50),
    ),
    recommendedMax: roundToStep(base + config.recommendedMaxOffset, 50),
  };
});

const calorieBandStyle = computed(() => rangeBandStyle(calorieRange.value));

const exerciseBandStyle = computed(() => rangeBandStyle(exerciseRange.value));

const displayCalorieIntakeGoal = computed({
  get() {
    return roundToStep(form.calorieIntakeGoal, 50);
  },
  set(value) {
    form.calorieIntakeGoal = Number(value);
  },
});

const displayExerciseCalorieGoal = computed({
  get() {
    return roundToStep(form.exerciseCalorieGoal, 50);
  },
  set(value) {
    form.exerciseCalorieGoal = Number(value);
  },
});

const warnings = computed(() => {
  const result = [];
  const calorieGoal = displayCalorieIntakeGoal.value;
  const exerciseGoal = displayExerciseCalorieGoal.value;

  if (calorieGoal < calorieRange.value.recommendedMin) {
    result.push(activeRangeConfig.value.calorie.lowWarning);
  }

  if (calorieGoal > calorieRange.value.recommendedMax) {
    result.push(activeRangeConfig.value.calorie.highWarning);
  }

  if (exerciseGoal < exerciseRange.value.recommendedMin) {
    result.push(activeRangeConfig.value.exercise.lowWarning);
  }

  if (exerciseGoal > exerciseRange.value.recommendedMax) {
    result.push(activeRangeConfig.value.exercise.highWarning);
  }

  return result;
});

const hasImpossibleValues = computed(() => {
  return form.calorieIntakeGoal <= 0 || form.exerciseCalorieGoal < 0;
});

const calorieTone = computed(() => {
  if (displayCalorieIntakeGoal.value < calorieRange.value.recommendedMin) {
    return "low";
  }

  if (displayCalorieIntakeGoal.value > calorieRange.value.recommendedMax) {
    return "loose";
  }

  return "balanced";
});

const exerciseTone = computed(() => {
  if (displayExerciseCalorieGoal.value < exerciseRange.value.recommendedMin) {
    return "low";
  }

  if (displayExerciseCalorieGoal.value > exerciseRange.value.recommendedMax) {
    return "high";
  }

  return "balanced";
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
    calorieIntakeGoal: displayCalorieIntakeGoal.value,
    exerciseCalorieGoal: displayExerciseCalorieGoal.value,
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

function rangeTickStyle(value, range) {
  const size = Math.max(range.max - range.min, 1);
  const left = ((value - range.min) / size) * 100;

  return {
    left: `${Math.max(0, Math.min(100, left))}%`,
  };
}
</script>

<template>
  <article class="daily-goal-setup-card">
    <div class="analysis-title">
      <i></i>
      <strong>목표 설정</strong>
    </div>

    <h2>{{ title }}</h2>
    <p>{{ description }}</p>

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
          <strong>{{ formatNumber(displayCalorieIntakeGoal) }} kcal</strong>
        </span>
        <div class="goal-range-wrap">
          <input
            v-model.number="form.calorieIntakeGoal"
            :min="calorieRange.min"
            :max="calorieRange.max"
            :style="calorieBandStyle"
            step="1"
            type="range"
          />
          <div class="goal-range-ticks">
            <small :style="rangeTickStyle(calorieRange.min, calorieRange)">{{
              formatNumber(calorieRange.min)
            }}</small>
            <small
              :style="
                rangeTickStyle(calorieRange.recommendedMin, calorieRange)
              "
              >{{ formatNumber(calorieRange.recommendedMin) }}</small
            >
            <small
              :style="
                rangeTickStyle(calorieRange.recommendedMax, calorieRange)
              "
              >{{ formatNumber(calorieRange.recommendedMax) }}</small
            >
            <small :style="rangeTickStyle(calorieRange.max, calorieRange)">{{
              formatNumber(calorieRange.max)
            }}</small>
          </div>
        </div>
        <input
          v-model.number="displayCalorieIntakeGoal"
          min="1"
          step="50"
          type="number"
        />
      </label>

      <label :class="['goal-slider-field', exerciseTone]">
        <span>
          <b>하루 운동 목표</b>
          <strong>{{ formatNumber(displayExerciseCalorieGoal) }} kcal</strong>
        </span>
        <div class="goal-range-wrap">
          <input
            v-model.number="form.exerciseCalorieGoal"
            :min="exerciseRange.min"
            :max="exerciseRange.max"
            :style="exerciseBandStyle"
            step="1"
            type="range"
          />
          <div class="goal-range-ticks">
            <small :style="rangeTickStyle(exerciseRange.min, exerciseRange)">{{
              formatNumber(exerciseRange.min)
            }}</small>
            <small
              :style="
                rangeTickStyle(exerciseRange.recommendedMin, exerciseRange)
              "
              >{{ formatNumber(exerciseRange.recommendedMin) }}</small
            >
            <small
              :style="
                rangeTickStyle(exerciseRange.recommendedMax, exerciseRange)
              "
              >{{ formatNumber(exerciseRange.recommendedMax) }}</small
            >
            <small :style="rangeTickStyle(exerciseRange.max, exerciseRange)">{{
              formatNumber(exerciseRange.max)
            }}</small>
          </div>
        </div>
        <input
          v-model.number="displayExerciseCalorieGoal"
          min="0"
          step="50"
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
      {{ isSaving ? "저장 중..." : submitLabel }}
    </button>
  </article>
</template>
