<script setup>
import AppShell from "./components/app/AppShell.vue";
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <AppShell
      v-if="route.meta.shell"
      :home-class="route.meta.shell.homeClass"
      :workspace-class="route.meta.shell.workspaceClass"
      :show-today-panel="Boolean(route.meta.shell.showTodayPanel)"
    >
      <component :is="Component" :key="route.fullPath" />
    </AppShell>

    <Transition v-else name="page-fade" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>
