<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
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
const router = useRouter();
const profileMenuOpen = ref(false);

const displayName = computed(() => {
  return authStore.user?.nickname || authStore.user?.email?.split("@")[0] || "사용자";
});

const avatarInitial = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase();
});

function toggleProfileMenu() {
  profileMenuOpen.value = !profileMenuOpen.value;
}

function closeProfileMenu() {
  profileMenuOpen.value = false;
}

function logout() {
  closeProfileMenu();
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
  <header class="app-topbar">
    <div class="topbar-brand">
      <div class="topbar-mark">
        <img src="/images/favicon.png" alt="" aria-hidden="true" />
      </div>
      <div>
        <strong>{{ APP_NAME }}</strong>
      </div>
    </div>

    <div class="topbar-profile">
      <button
        type="button"
        class="topbar-profile-button"
        :class="{ open: profileMenuOpen }"
        :aria-expanded="profileMenuOpen"
        @click="toggleProfileMenu"
      >
        <span class="topbar-avatar">{{ avatarInitial }}</span>
        <span>{{ displayName }}</span>
        <i :class="profileMenuOpen ? 'pi pi-chevron-up' : 'pi pi-chevron-down'" aria-hidden="true"></i>
      </button>

      <button
        v-if="profileMenuOpen"
        type="button"
        class="topbar-menu-backdrop"
        aria-label="프로필 메뉴 닫기"
        @click="closeProfileMenu"
      ></button>

      <Transition name="topbar-profile-menu">
        <div v-if="profileMenuOpen" class="topbar-profile-menu" role="menu">
          <button type="button" role="menuitem" class="danger" @click="logout">
            <i class="pi pi-sign-out"></i>
            로그아웃
          </button>
        </div>
      </Transition>
    </div>
  </header>
</template>
