<script setup>
import { ref } from "vue";

defineProps({
  label: {
    type: String,
    required: true,
  },
  icon: {
    type: String,
    required: true,
  },
  type: {
    type: String,
    default: "text",
  },
  modelValue: {
    type: [String, Number],
    default: "",
  },
  placeholder: {
    type: String,
    default: "",
  },
  autocomplete: {
    type: String,
    default: "",
  },
  focused: {
    type: Boolean,
    default: false,
  },
  showActionIcon: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue"]);
const isFocused = ref(false);
</script>

<template>
  <label>
    <span>{{ label }}</span>
    <div class="field-shell" :class="{ focused: focused || isFocused }">
      <i :class="icon"></i>
      <input
        :value="modelValue"
        :type="type"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        @blur="isFocused = false"
        @focus="isFocused = true"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <i v-if="showActionIcon" class="pi pi-eye field-action"></i>
    </div>
  </label>
</template>
