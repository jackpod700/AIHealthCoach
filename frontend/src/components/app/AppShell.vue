<script setup>
import AppSideNav from "./AppSideNav.vue";
import AppTodayPanel from "./AppTodayPanel.vue";
import AppTopBar from "./AppTopBar.vue";

defineProps({
  homeClass: {
    type: String,
    required: true,
  },
  workspaceClass: {
    type: String,
    required: true,
  },
  showTodayPanel: {
    type: Boolean,
    default: false,
  },
});
</script>

<template>
  <main :class="homeClass">
    <AppTopBar />
    <div class="app-shell-body" :class="{ 'with-today-panel': showTodayPanel }">
      <AppSideNav />
      <section :class="workspaceClass">
        <slot />
      </section>
      <AppTodayPanel v-if="showTodayPanel" />
    </div>
    <slot name="after" />
  </main>
</template>
