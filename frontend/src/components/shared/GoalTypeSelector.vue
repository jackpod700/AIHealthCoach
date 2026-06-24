<script setup>
defineProps({
  modelValue: {
    type: String,
    required: true,
  },
  options: {
    type: Array,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue", "select"]);

function selectGoal(goalType) {
  emit("update:modelValue", goalType);
  emit("select", goalType);
}
</script>

<template>
  <div class="profile-goal-card-grid">
    <button
      v-for="goal in options"
      :key="goal.value"
      type="button"
      :class="{ active: modelValue === goal.value }"
      :disabled="disabled"
      @click="selectGoal(goal.value)"
    >
      <div class="profile-goal-card-head">
        <span class="profile-goal-icon">
          <i :class="goal.icon"></i>
        </span>
        <div class="profile-goal-card-copy">
          <strong>{{ goal.title }}</strong>
          <span>{{ goal.desc }}</span>
        </div>
        <span class="profile-goal-check">
          <i class="pi pi-check"></i>
        </span>
      </div>
    </button>
  </div>
</template>
