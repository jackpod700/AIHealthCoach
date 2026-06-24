<script setup>
defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
  subtitle: {
    type: String,
    default: "BabStroy",
  },
});
</script>

<template>
  <div :class="compact ? 'signup-brand' : 'login-brand'">
    <div class="brand-mark">
      <i class="pi pi-briefcase"></i>
    </div>
    <div v-if="!compact">
      <h1>BabStroy</h1>
      <p>{{ subtitle }}</p>
    </div>
    <strong v-else>BabStroy 시작하기</strong>
  </div>
</template>
