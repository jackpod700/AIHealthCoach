<script setup>
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const navItems = [
  { label: "대화", icon: "pi pi-comment", to: "/chat" },
  { label: "캘린더", icon: "pi pi-calendar", to: "/calendar" },
  { label: "일일 기록", icon: "pi pi-file", to: "/records" },
  { label: "음식 검색", icon: "pi pi-search", to: "/foods" },
  { label: "프로필", icon: "pi pi-user", to: "/profile" },
];

function isActive(item) {
  return route.path === item.to;
}

function navigateTo(path) {
  if (route.path !== path) {
    router.push(path);
  }
}
</script>

<template>
  <aside class="app-sidenav">
    <nav class="sidenav-nav" aria-label="메인 메뉴">
      <button
        v-for="item in navItems"
        :key="item.label"
        type="button"
        :class="{ active: isActive(item) }"
        @click="navigateTo(item.to)"
      >
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
      </button>
      <button
        v-if="authStore.isAdmin"
        type="button"
        :class="{ active: route.path === '/admin' }"
        @click="navigateTo('/admin')"
      >
        <i class="pi pi-chart-line"></i>
        <span>관리자</span>
      </button>
    </nav>
  </aside>
</template>
