<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { useAdminStore } from "../../stores/adminStore";

const adminStore = useAdminStore();
const REFRESH_INTERVAL_MS = 5000;
const lastUpdatedAt = ref(null);
let refreshTimerId = null;

onMounted(() => {
  loadDashboard();
  refreshTimerId = window.setInterval(loadDashboard, REFRESH_INTERVAL_MS);
});

onUnmounted(() => {
  if (refreshTimerId) {
    window.clearInterval(refreshTimerId);
  }
});

const users = computed(() => adminStore.dashboard?.users);

async function loadDashboard() {
  await adminStore.loadDashboard();

  if (!adminStore.error) {
    lastUpdatedAt.value = new Date();
  }
}

function number(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  return Number(value).toLocaleString("ko-KR");
}

function lastUpdatedLabel() {
  if (!lastUpdatedAt.value) {
    return "-";
  }
  return lastUpdatedAt.value.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}
</script>

<template>
  <div class="admin-home">
    <AppSidebar />

    <main class="admin-workspace">
      <header class="admin-header">
        <div>
          <p class="section-eyebrow">ADMIN DASHBOARD</p>
          <h1>관리자 대시보드</h1>
          <small class="admin-live-status">
            5초마다 자동 갱신 · 마지막 갱신 {{ lastUpdatedLabel() }}
          </small>
        </div>
        <button type="button" class="admin-refresh-button" :disabled="adminStore.isLoading" @click="loadDashboard">
          <i class="pi pi-refresh"></i>
          <span>{{ adminStore.isLoading ? "갱신 중" : "새로고침" }}</span>
        </button>
      </header>

      <section class="admin-content">
        <div v-if="adminStore.error" class="admin-error">
          {{ adminStore.error }}
        </div>

        <div v-if="!users && adminStore.isLoading" class="admin-loading">
          사용자 현황을 불러오고 있어요.
        </div>

        <section v-if="users" class="admin-user-status">
          <article class="admin-user-card">
            <span>전체 사용자</span>
            <strong>{{ number(users.totalUsers) }}</strong>
            <small>가입된 전체 계정 수</small>
          </article>
          <article class="admin-user-card">
            <span>오늘 가입</span>
            <strong>{{ number(users.todaySignups) }}</strong>
            <small>오늘 생성된 신규 계정</small>
          </article>
          <article class="admin-user-card">
            <span>최근 5분 활성</span>
            <strong>{{ number(users.activeUsers5m) }}</strong>
            <small>최근 5분 내 인증 API 요청 사용자</small>
          </article>
        </section>
      </section>
    </main>
  </div>
</template>
