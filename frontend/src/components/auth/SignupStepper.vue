<script setup>
defineProps({
  steps: {
    type: Array,
    required: true,
  },
  activeStep: {
    type: Number,
    required: true,
  },
});
</script>

<template>
  <ol class="signup-steps">
    <template v-for="step in steps" :key="step.id">
      <li :class="{ active: activeStep === step.id, done: activeStep > step.id }">
        <span>
          <i v-if="activeStep > step.id" class="pi pi-check"></i>
          <b v-else>{{ step.id }}</b>
        </span>
        {{ step.label }}
      </li>
      <i
        v-if="step.id < steps.length"
        class="step-line"
        :class="{ filled: activeStep > step.id }"
      ></i>
    </template>
  </ol>
</template>
