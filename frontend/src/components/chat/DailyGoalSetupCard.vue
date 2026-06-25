<script setup>
import { computed, reactive, ref, watch } from "vue";
import UserGoalSettingsCard from "../shared/UserGoalSettingsCard.vue";
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
const pendingGoalType = ref("");

const recommendationBase = reactive({
  calorieIntakeGoal: 1800,
  exerciseCalorieGoal: 300,
});

const GOOD_TARGET_MIN_RATIO = 0.9;
const GOOD_TARGET_MAX_RATIO = 1.1;

const GOAL_RANGE_CONFIG = {
  WEIGHT_LOSS: {
    calorie: {
      minOffset: -500,
      maxOffset: 400,
    },
    exercise: {
      minOffset: -150,
      maxOffset: 350,
    },
  },
  MAINTENANCE: {
    calorie: {
      minOffset: -500,
      maxOffset: 500,
    },
    exercise: {
      minOffset: -150,
      maxOffset: 300,
    },
  },
  MUSCLE_GAIN: {
    calorie: {
      minOffset: -400,
      maxOffset: 700,
    },
    exercise: {
      minOffset: -100,
      maxOffset: 400,
    },
  },
};

const activeRangeConfig = computed(() => {
  return GOAL_RANGE_CONFIG[form.goalType] || GOAL_RANGE_CONFIG.WEIGHT_LOSS;
});

const selectedGoalType = computed(() => {
  return pendingGoalType.value || form.goalType;
});

const calorieRange = computed(() => {
  const base = recommendationBase.calorieIntakeGoal;
  const config = activeRangeConfig.value.calorie;

  return {
    min: Math.max(1000, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
  };
});

const exerciseRange = computed(() => {
  const base = recommendationBase.exerciseCalorieGoal;
  const config = activeRangeConfig.value.exercise;

  return {
    min: Math.max(0, roundToStep(base + config.minOffset, 50)),
    max: roundToStep(base + config.maxOffset, 50),
  };
});

const calorieSliderStyle = computed(() =>
  goalSliderStyle(
    form.calorieIntakeGoal,
    calorieRange.value.min,
    calorieRange.value.max,
    calorieTone.value,
  ),
);

const exerciseSliderStyle = computed(() =>
  goalSliderStyle(
    form.exerciseCalorieGoal,
    exerciseRange.value.min,
    exerciseRange.value.max,
    exerciseTone.value,
  ),
);

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

const hasImpossibleValues = computed(() => {
  return form.calorieIntakeGoal <= 0 || form.exerciseCalorieGoal < 0;
});

const calorieTone = computed(() => {
  return goalTargetStatus(
    displayCalorieIntakeGoal.value,
    recommendationBase.calorieIntakeGoal,
  );
});

const exerciseTone = computed(() => {
  return goalTargetStatus(
    displayExerciseCalorieGoal.value,
    recommendationBase.exerciseCalorieGoal,
  );
});

const calorieStatusLabel = computed(() => goalStatusLabel(calorieTone.value));
const exerciseStatusLabel = computed(() => goalStatusLabel(exerciseTone.value));

const goalFooterMessage = computed(() => {
  if (props.recommendationError) {
    return "추천값을 불러오지 못했어요. 저장은 가능하지만 추천 범위 판단은 잠시 사용할 수 없어요.";
  }

  if (calorieTone.value === "good" && exerciseTone.value === "good") {
    return "추천 범위 안의 좋은 목표예요.";
  }

  return "저장은 가능하지만 추천 범위를 벗어난 목표가 있어요.";
});

const calorieGoalGuideMessage = computed(() => {
  return goalGuideMessage(calorieTone.value, "섭취");
});

const exerciseGoalGuideMessage = computed(() => {
  return goalGuideMessage(exerciseTone.value, "운동");
});

function goalTargetStatus(value, recommended) {
  const target = Number(recommended);

  if (!Number.isFinite(target) || target <= 0) {
    return "unavailable";
  }

  if (value < target * GOOD_TARGET_MIN_RATIO) {
    return "low";
  }

  if (value > target * GOOD_TARGET_MAX_RATIO) {
    return "high";
  }

  return "good";
}

function goalStatusLabel(status) {
  if (status === "low") {
    return "너무 낮음";
  }

  if (status === "high") {
    return "너무 높음";
  }

  if (status === "unavailable") {
    return "추천 확인 중";
  }

  return "좋은 목표";
}

function goalGuideMessage(status, metricLabel) {
  if (status === "low") {
    return `${metricLabel} 목표가 추천보다 낮아요. 조금 올려보면 더 균형 잡힌 목표가 돼요.`;
  }

  if (status === "high") {
    return `${metricLabel} 목표가 추천보다 높아요. 부담이 크지 않도록 낮추는 것도 좋아요.`;
  }

  if (status === "unavailable") {
    return "추천값을 기준으로 목표 범위를 확인하는 중이에요.";
  }

  return `${metricLabel} 목표가 추천 범위 안에 있어요. 좋은 목표입니다.`;
}

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

    if (pendingGoalType.value) {
      form.goalType = pendingGoalType.value;
      pendingGoalType.value = "";
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

watch(
  () => props.recommendationError,
  (recommendationError) => {
    if (recommendationError) {
      pendingGoalType.value = "";
    }
  },
);

function selectGoal(goalType) {
  if (goalType === selectedGoalType.value || props.isLoadingRecommendation) {
    return;
  }

  pendingGoalType.value = goalType;
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

function goalSliderPercent(value, min, max) {
  return Math.min(
    Math.max(((Number(value) - min) / Math.max(max - min, 1)) * 100, 0),
    100,
  );
}

function goalSliderStyle(value, min, max, tone) {
  const percent = goalSliderPercent(value, min, max);
  const color =
    tone === "high"
      ? "#d6453f"
      : tone === "low"
        ? "#d89b2f"
        : tone === "unavailable"
          ? "#8a908a"
        : "#2f8a55";

  return {
    "--goal-slider-percent": `${percent}%`,
    "--goal-slider-color": color,
  };
}
</script>

<template>
  <UserGoalSettingsCard
    :model-value="selectedGoalType"
    :options="goalOptions"
    :title="title"
    compact
    :show-edit-button="false"
    :show-overview="false"
    :selector-disabled="isLoadingRecommendation"
    @select="selectGoal"
  >
    <template #after-selector>
      <p class="daily-goal-card-description">{{ description }}</p>
    </template>

    <div v-if="recommendationError" class="daily-goal-inline-error">
      {{ recommendationError }}
    </div>

    <div class="profile-goal-inline-editor">
      <label :class="calorieTone">
        <div class="profile-goal-slider-copy">
          <span>
            하루 섭취 목표
            <b class="profile-goal-status" :class="calorieTone">
              {{ calorieStatusLabel }}
            </b>
          </span>
          <p>{{ calorieGoalGuideMessage }}</p>
        </div>
        <strong>{{ formatNumber(displayCalorieIntakeGoal) }} kcal</strong>
        <div class="profile-goal-slider" :style="calorieSliderStyle">
          <div class="profile-goal-slider-track"></div>
          <div class="profile-goal-slider-fill"></div>
          <div class="profile-goal-slider-thumb">
            <i></i>
            <b>{{ formatNumber(displayCalorieIntakeGoal) }}</b>
          </div>
          <input
            v-model.number="form.calorieIntakeGoal"
            :min="calorieRange.min"
            :max="calorieRange.max"
            step="1"
            type="range"
          />
        </div>
      </label>

      <label :class="exerciseTone">
        <div class="profile-goal-slider-copy">
          <span>
            하루 운동 목표
            <b class="profile-goal-status" :class="exerciseTone">
              {{ exerciseStatusLabel }}
            </b>
          </span>
          <p>{{ exerciseGoalGuideMessage }}</p>
        </div>
        <strong>{{ formatNumber(displayExerciseCalorieGoal) }} kcal</strong>
        <div class="profile-goal-slider" :style="exerciseSliderStyle">
          <div class="profile-goal-slider-track"></div>
          <div class="profile-goal-slider-fill"></div>
          <div class="profile-goal-slider-thumb">
            <i></i>
            <b>{{ formatNumber(displayExerciseCalorieGoal) }}</b>
          </div>
          <input
            v-model.number="form.exerciseCalorieGoal"
            :min="exerciseRange.min"
            :max="exerciseRange.max"
            step="1"
            type="range"
          />
        </div>
      </label>
    </div>

    <div v-if="saveError" class="daily-goal-inline-error">
      {{ saveError }}
    </div>

    <p class="daily-goal-card-description">{{ goalFooterMessage }}</p>

    <button
      class="daily-goal-save-button"
      type="button"
      :disabled="isLoadingRecommendation || isSaving || hasImpossibleValues"
      @click="saveGoal"
    >
      {{ isSaving ? "저장 중..." : submitLabel }}
    </button>
  </UserGoalSettingsCard>
</template>
