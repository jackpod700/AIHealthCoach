<script setup>
import GoalTypeSelector from "./GoalTypeSelector.vue";

defineProps({
  id: {
    type: String,
    default: "",
  },
  modelValue: {
    type: String,
    required: true,
  },
  options: {
    type: Array,
    required: true,
  },
  title: {
    type: String,
    default: "목표 설정",
  },
  editing: {
    type: Boolean,
    default: false,
  },
  selectorDisabled: {
    type: Boolean,
    default: false,
  },
  showHeader: {
    type: Boolean,
    default: true,
  },
  showEditButton: {
    type: Boolean,
    default: true,
  },
  showOverview: {
    type: Boolean,
    default: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
  calorieGoal: {
    type: [Number, String],
    default: null,
  },
  exerciseGoal: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "select", "toggle-edit"]);

function formatNumber(value) {
  return Math.round(Number(value) || 0).toLocaleString("ko-KR");
}
</script>

<template>
  <section :id="id || undefined" class="profile-goal-card" :class="{ compact }">
    <div v-if="showHeader" class="profile-section-head">
      <div class="profile-section-title">{{ title }}</div>
      <button
        v-if="showEditButton"
        type="button"
        class="profile-section-edit"
        :aria-pressed="editing"
        :aria-label="`${title} 수정`"
        @click="emit('toggle-edit')"
      >
        <i :class="editing ? 'pi pi-times' : 'pi pi-pencil'"></i>
      </button>
    </div>

    <GoalTypeSelector
      :model-value="modelValue"
      :options="options"
      :disabled="selectorDisabled"
      @update:model-value="emit('update:modelValue', $event)"
      @select="emit('select', $event)"
    />

    <slot name="after-selector">
      <div v-if="showOverview" class="profile-goal-overview">
        <div>
          <span class="profile-goal-overview-icon calorie">
            <i class="pi pi-apple"></i>
          </span>
          <div>
            <span>목표 섭취 칼로리</span>
            <strong>{{ formatNumber(calorieGoal) }} kcal</strong>
          </div>
        </div>
        <div>
          <span class="profile-goal-overview-icon exercise">
            <i class="pi pi-fire"></i>
          </span>
          <div>
            <span>목표 소모 칼로리</span>
            <strong>{{ formatNumber(exerciseGoal) }} kcal</strong>
          </div>
        </div>
      </div>
    </slot>

    <slot />
  </section>
</template>
