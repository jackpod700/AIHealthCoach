<script setup>
defineProps({
  goal: {
    type: Object,
    required: true,
  },
  selected: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["select"]);
</script>

<template>
  <button class="goal-card" :class="{ selected }" type="button" @click="emit('select', goal.value)">
    <i :class="goal.icon"></i>
    <span class="goal-card-copy">
      <strong>{{ goal.title }}</strong>
      <small>{{ goal.desc }}</small>
    </span>
    <span class="goal-check" :class="{ selected }">
      <i v-if="selected" class="pi pi-check"></i>
    </span>
  </button>
</template>
