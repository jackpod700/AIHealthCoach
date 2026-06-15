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

const dashboard = computed(() => adminStore.dashboard);

const summaryCards = computed(() => {
  const data = dashboard.value;

  return [
    {
      label: "CPU",
      value: percent(data?.server?.cpuUsagePercent),
      detail: "현재 서버 부하",
      icon: "pi pi-microchip",
    },
    {
      label: "RAM",
      value: memoryRatio(data?.server?.systemMemoryUsedMb, data?.server?.systemMemoryTotalMb),
      detail: "시스템 메모리",
      icon: "pi pi-server",
    },
    {
      label: "활성 사용자",
      value: number(data?.users?.activeUsers5m),
      detail: "최근 5분",
      icon: "pi pi-users",
    },
    {
      label: "AI 평균 응답",
      value: `${number(data?.ai?.averageLatencyMsToday)}ms`,
      detail: "오늘 기준",
      icon: "pi pi-bolt",
    },
  ];
});

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
  return Number(value).toLocaleString("ko-KR", { maximumFractionDigits: 1 });
}

function percent(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${Number(value).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}%`;
}

function memoryRatio(used, total) {
  if (used === null || used === undefined || total === null || total === undefined) {
    return "-";
  }
  return `${number(used)} / ${number(total)}MB`;
}

function secondsToUptime(seconds) {
  if (!seconds) {
    return "-";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${hours}시간 ${minutes}분`;
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

        <div v-if="!dashboard && adminStore.isLoading" class="admin-loading">
          관리자 지표를 불러오고 있어요.
        </div>

        <template v-if="dashboard">
          <section class="admin-summary-grid">
            <article v-for="card in summaryCards" :key="card.label" class="admin-summary-card">
              <i :class="card.icon"></i>
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
              <small>{{ card.detail }}</small>
            </article>
          </section>

          <section class="admin-panel-grid">
            <article class="admin-panel">
              <p class="section-eyebrow">SERVER</p>
              <h2>서버 상태</h2>
              <dl>
                <div>
                  <dt>CPU 사용률</dt>
                  <dd>{{ percent(dashboard.server.cpuUsagePercent) }}</dd>
                </div>
                <div>
                  <dt>시스템 메모리</dt>
                  <dd>{{ memoryRatio(dashboard.server.systemMemoryUsedMb, dashboard.server.systemMemoryTotalMb) }}</dd>
                </div>
                <div>
                  <dt>JVM 메모리</dt>
                  <dd>{{ memoryRatio(dashboard.server.jvmMemoryUsedMb, dashboard.server.jvmMemoryMaxMb) }}</dd>
                </div>
                <div>
                  <dt>Uptime</dt>
                  <dd>{{ secondsToUptime(dashboard.server.uptimeSeconds) }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-panel">
              <p class="section-eyebrow">TRAFFIC</p>
              <h2>API 트래픽</h2>
              <dl>
                <div>
                  <dt>최근 1분 요청</dt>
                  <dd>{{ number(dashboard.traffic.requestCount1m) }}</dd>
                </div>
                <div>
                  <dt>최근 5분 요청</dt>
                  <dd>{{ number(dashboard.traffic.requestCount5m) }}</dd>
                </div>
                <div>
                  <dt>최근 1시간 요청</dt>
                  <dd>{{ number(dashboard.traffic.requestCount1h) }}</dd>
                </div>
                <div>
                  <dt>5분 평균 응답</dt>
                  <dd>{{ number(dashboard.traffic.averageResponseMs5m) }}ms</dd>
                </div>
                <div>
                  <dt>4xx / 5xx</dt>
                  <dd>{{ number(dashboard.traffic.clientErrorCount5m) }} / {{ number(dashboard.traffic.serverErrorCount5m) }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-panel">
              <p class="section-eyebrow">AI</p>
              <h2>AI 사용량</h2>
              <dl>
                <div>
                  <dt>오늘 요청</dt>
                  <dd>{{ number(dashboard.ai.requestCountToday) }}</dd>
                </div>
                <div>
                  <dt>성공 / 실패</dt>
                  <dd>{{ number(dashboard.ai.successCountToday) }} / {{ number(dashboard.ai.failureCountToday) }}</dd>
                </div>
                <div>
                  <dt>평균 응답</dt>
                  <dd>{{ number(dashboard.ai.averageLatencyMsToday) }}ms</dd>
                </div>
                <div>
                  <dt>Input tokens</dt>
                  <dd>{{ number(dashboard.ai.inputTokensToday) }}</dd>
                </div>
                <div>
                  <dt>Output tokens</dt>
                  <dd>{{ number(dashboard.ai.outputTokensToday) }}</dd>
                </div>
                <div>
                  <dt>Total tokens</dt>
                  <dd>{{ number(dashboard.ai.totalTokensToday) }}</dd>
                </div>
              </dl>
            </article>

            <article class="admin-panel">
              <p class="section-eyebrow">USERS</p>
              <h2>사용자 현황</h2>
              <dl>
                <div>
                  <dt>전체 사용자</dt>
                  <dd>{{ number(dashboard.users.totalUsers) }}</dd>
                </div>
                <div>
                  <dt>오늘 가입</dt>
                  <dd>{{ number(dashboard.users.todaySignups) }}</dd>
                </div>
                <div>
                  <dt>최근 5분 활성</dt>
                  <dd>{{ number(dashboard.users.activeUsers5m) }}</dd>
                </div>
              </dl>
            </article>
          </section>
        </template>
      </section>
    </main>
  </div>
</template>
