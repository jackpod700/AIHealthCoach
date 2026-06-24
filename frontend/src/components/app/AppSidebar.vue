<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { APP_NAME } from "../../constants/brand";
import { useAuthStore } from "../../stores/authStore";
import { useChatStore } from "../../stores/chatStore";
import { useDailyGoalStore } from "../../stores/dailyGoalStore";
import { useExerciseStore } from "../../stores/exerciseStore";
import { useMealStore } from "../../stores/mealStore";
import { useProfileStore } from "../../stores/profileStore";
import { useWeightRecordStore } from "../../stores/weightRecordStore";

const authStore = useAuthStore();
const chatStore = useChatStore();
const dailyGoalStore = useDailyGoalStore();
const exerciseStore = useExerciseStore();
const mealStore = useMealStore();
const profileStore = useProfileStore();
const weightRecordStore = useWeightRecordStore();
const route = useRoute();
const router = useRouter();
const logoutPromptOpen = ref(false);

const navItems = [
  { label: "대화", icon: "pi pi-comment", to: "/chat" },
  { label: "캘린더", icon: "pi pi-calendar", to: "/calendar" },
  { label: "일일 기록", icon: "pi pi-file", to: "/records" },
  { label: "음식 검색", icon: "pi pi-search", to: "/foods" },
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

  if (goalType === "MAINTENANCE") {
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

function navigateTo(path) {
  if (path !== "#" && route.path !== path) {
    router.push(path);
  }
}

function toggleLogoutPrompt() {
  logoutPromptOpen.value = !logoutPromptOpen.value;
}

function closeLogoutPrompt() {
  logoutPromptOpen.value = false;
}

function logout() {
  logoutPromptOpen.value = false;
  authStore.logout();
  chatStore.clearMessages();
  dailyGoalStore.clearDailyGoal();
  exerciseStore.clearExercise();
  mealStore.clearMeals();
  profileStore.clearProfile();
  weightRecordStore.clearWeightRecords();
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
        <strong>{{ APP_NAME }}</strong>
      </div>
    </div>

    <nav class="sidebar-nav" aria-label="메인 메뉴">
      <p>메뉴</p>
      <button
        v-for="item in navItems"
        :key="item.label"
        type="button"
        :class="{ active: isActive(item), disabled: item.to === '#' }"
        :disabled="item.to === '#'"
        @click="navigateTo(item.to)"
      >
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
        <em v-if="item.needsApi">API 필요</em>
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

    <div
      class="sidebar-user"
      :class="{ open: logoutPromptOpen }"
      role="button"
      tabindex="0"
      @click="toggleLogoutPrompt"
      @keydown.enter.prevent="toggleLogoutPrompt"
      @keydown.space.prevent="toggleLogoutPrompt"
    >
      <div class="sidebar-avatar">{{ avatarInitial }}</div>
      <div>
        <strong>{{ displayName }}</strong>
        <span>{{ goalLabel }}</span>
      </div>

      <i class="pi pi-sign-out sidebar-user-action" aria-hidden="true"></i>

      <div v-if="logoutPromptOpen" class="sidebar-logout-popover" role="dialog" aria-label="로그아웃 확인" @click.stop>
        <div class="sidebar-logout-icon">
          <i class="pi pi-sign-out"></i>
        </div>
        <strong>로그아웃하시겠습니까?</strong>
        <span>현재 계정에서 나가고 로그인 화면으로 이동합니다.</span>
        <div class="sidebar-logout-actions">
          <button class="sidebar-logout-cancel" type="button" @click="closeLogoutPrompt">취소</button>
          <button class="sidebar-logout-confirm" type="button" @click="logout">로그아웃</button>
        </div>
      </div>
    </div>
  </aside>
</template>
