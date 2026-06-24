<script setup>
import { APP_NAME, APP_START_LABEL } from "../../constants/brand";

defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
  subtitle: {
    type: String,
    default: APP_NAME,
  },
});
</script>

<template>
  <div :class="compact ? 'signup-brand' : 'login-brand'">
    <div class="brand-mark">
      <i class="pi pi-briefcase"></i>
    </div>
    <div v-if="!compact">
      <h1>{{ APP_NAME }}</h1>
      <p>{{ subtitle }}</p>
    </div>
    <strong v-else>{{ APP_START_LABEL }}</strong>
  </div>
</template>
