<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";

const authStore = useAuthStore();
const profileStore = useProfileStore();
const route = useRoute();
const router = useRouter();

const navItems = [
  { label: "대화", icon: "pi pi-comment", to: "/chat" },
  { label: "캘린더", icon: "pi pi-calendar", to: "#", needsApi: true },
  { label: "일일 기록", icon: "pi pi-file", to: "#", needsApi: true },
  { label: "프로필", icon: "pi pi-user", to: "/profile" },
];

const displayName = computed(() => {
  return authStore.user?.nickname || authStore.user?.email?.split("@")[0] || "사용자";
});

const avatarInitial = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase();
});

const goalLabel = computed(() => {
  const goalType = profileStore.profile?.goalType;

  if (goalType === "WEIGHT_LOSS") {
    return "감량 목표";
  }

  if (goalType === "MAINTAIN") {
    return "유지 목표";
  }

  if (goalType === "MUSCLE_GAIN") {
    return "근육 증가 목표";
  }

  return "프로필 API 연결 필요";
});

function isActive(item) {
  return item.to !== "#" && route.path === item.to;
}

function logout() {
  authStore.logout();
  profileStore.clearProfile();
  router.push("/login");
}
</script>

<template>
  <aside class="app-sidebar">
    <div class="sidebar-brand">
      <div class="sidebar-mark">
        <i class="pi pi-briefcase"></i>
      </div>
      <div>
        <strong>헬스 코치</strong>
        <span>AI Health Coach</span>
      </div>
    </div>

    <nav class="sidebar-nav" aria-label="메인 메뉴">
      <p>메뉴</p>
      <RouterLink
        v-for="item in navItems"
        :key="item.label"
        :class="{ active: isActive(item), disabled: item.to === '#' }"
        :to="item.to"
      >
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
        <em v-if="item.needsApi">API 필요</em>
      </RouterLink>
    </nav>

    <div class="sidebar-user">
      <div class="sidebar-avatar">{{ avatarInitial }}</div>
      <div>
        <strong>{{ displayName }}</strong>
        <span>{{ goalLabel }}</span>
      </div>
      <button class="sidebar-logout" type="button" @click="logout">로그아웃</button>
    </div>
  </aside>
</template>
