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
    <span v-if="selected" class="goal-check">
      <i class="pi pi-check"></i>
    </span>
    <strong>{{ goal.title }}</strong>
    <small>{{ goal.desc }}</small>
  </button>
</template>
