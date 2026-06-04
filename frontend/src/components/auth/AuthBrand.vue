<script setup>
defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
  subtitle: {
    type: String,
    default: "AI Health Coach",
  },
});
</script>

<template>
  <div :class="compact ? 'signup-brand' : 'login-brand'">
    <div class="brand-mark">
      <i class="pi pi-briefcase"></i>
    </div>
    <div v-if="!compact">
      <h1>헬스 코치</h1>
      <p>{{ subtitle }}</p>
    </div>
    <strong v-else>헬스 코치 시작하기</strong>
  </div>
</template>
