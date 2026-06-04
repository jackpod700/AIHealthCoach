<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { marked } from "marked";
import { useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { useAuthStore } from "../../stores/authStore";
import { useChatStore } from "../../stores/chatStore";
import { useProfileStore } from "../../stores/profileStore";

const authStore = useAuthStore();
const chatStore = useChatStore();
const profileStore = useProfileStore();
const router = useRouter();
const message = ref("");
const threadRef = ref(null);

const todayLabel = computed(() => {
  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "long",
  }).format(new Date());
});

const displayMessages = computed(() => {
  return chatStore.orderedMessages;
});

const hasMessages = computed(() => {
  return displayMessages.value.length > 0;
});

marked.setOptions({
  breaks: true,
  gfm: true,
});

onMounted(async () => {
  await Promise.all([
    chatStore.loadMessages(),
    profileStore.loadProfile(),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  await scrollToBottom();
});

async function sendMessage() {
  const content = message.value.trim();

  if (!content || chatStore.isSending) {
    return;
  }

  message.value = "";
  await chatStore.sendMessage(content);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  await scrollToBottom();
}

async function scrollToBottom() {
  await nextTick();

  if (threadRef.value) {
    threadRef.value.scrollTop = threadRef.value.scrollHeight;
  }
}

function isUserMessage(chatMessage) {
  return chatMessage.role === "USER";
}

function formatMessageTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function renderMarkdown(content = "") {
  return sanitizeHtml(marked.parse(content));
}

function sanitizeHtml(html = "") {
  const template = document.createElement("template");
  template.innerHTML = html;

  const allowedTags = new Set([
    "P",
    "STRONG",
    "EM",
    "UL",
    "OL",
    "LI",
    "TABLE",
    "THEAD",
    "TBODY",
    "TR",
    "TH",
    "TD",
    "H1",
    "H2",
    "H3",
    "H4",
    "BR",
    "CODE",
    "PRE",
  ]);

  template.content.querySelectorAll("*").forEach((element) => {
    if (!allowedTags.has(element.tagName)) {
      element.replaceWith(document.createTextNode(element.textContent || ""));
      return;
    }

    [...element.attributes].forEach((attribute) => {
      element.removeAttribute(attribute.name);
    });
  });

  return template.innerHTML;
}
</script>

<template>
  <main class="chat-home">
    <AppSidebar />

    <section class="chat-workspace">
      <header class="chat-header">
        <div>
          <p class="deco">Today's Coaching</p>
          <h1>오늘의 코칭</h1>
        </div>
        <div class="streak-chip api-required-chip">
          <i></i>
          연속 기록 API 연결 필요
        </div>
      </header>

      <div class="chat-body">
        <section class="chat-thread">
          <div class="chat-scroll" ref="threadRef">
            <div class="day-pill">오늘 · {{ todayLabel }}</div>

            <div v-if="chatStore.isLoading" class="chat-state-card">
              채팅 기록을 불러오는 중입니다...
            </div>

            <div v-else-if="chatStore.error" class="chat-state-card error">
              {{ chatStore.error }}
            </div>

            <div v-else-if="!hasMessages" class="chat-state-card">
              아직 대화 기록이 없어요. 식단이나 운동을 편하게 입력해보세요.
            </div>

            <template v-for="chatMessage in displayMessages" :key="chatMessage.id || chatMessage.clientId">
              <div v-if="isUserMessage(chatMessage)" class="message-row user">
                <div class="message-bubble user-bubble">
                  {{ chatMessage.content }}
                  <span>{{ formatMessageTime(chatMessage.createdAt) }}</span>
                </div>
              </div>

              <div v-else class="message-row coach">
                <div class="coach-icon">
                  <i class="pi pi-briefcase"></i>
                </div>
                <article class="assistant-card" :class="{ pending: chatMessage.pending, failed: chatMessage.failed }">
                  <div class="analysis-title">
                    <i></i>
                    <strong>AI 코치 답변</strong>
                  </div>
                  <div class="markdown-content" v-html="renderMarkdown(chatMessage.content)"></div>
                  <time>{{ formatMessageTime(chatMessage.createdAt) }}</time>
                </article>
              </div>
            </template>

            <div class="chat-api-note">
              식사 분석 표, 후보 음식, 확정 저장 UI는 채팅 응답의 구조화 데이터 API 연결이 필요합니다.
            </div>
          </div>

          <form class="chat-composer" @submit.prevent="sendMessage">
            <input v-model="message" placeholder="식단이나 운동을 편하게 기록해보세요..." />
            <button type="button" aria-label="추가">
              <i class="pi pi-plus"></i>
            </button>
            <button type="button" aria-label="음성 입력">
              <i class="pi pi-microphone"></i>
            </button>
            <button class="send-button" type="submit" aria-label="전송" :disabled="chatStore.isSending">
              <i class="pi pi-send"></i>
            </button>
          </form>

          <p class="composer-note">AI 코치는 참고용 가이드를 제공해요 · 의학적 진단은 전문가와 상담하세요</p>
        </section>

        <aside class="today-panel">
          <div class="today-head">
            <p class="deco">Today</p>
            <span>{{ todayLabel }}</span>
          </div>

          <section class="api-needed-panel">
            <strong>오늘 식단 요약 API 연결 필요</strong>
            <p>
              칼로리, 탄수화물, 단백질, 지방 합계는 일별 식단 조회 API가 연결되면 표시할 수 있습니다.
            </p>
          </section>

          <section class="calorie-card pending-api">
            <span>오늘 섭취 칼로리</span>
            <strong>-<small>/ 목표 kcal</small></strong>
            <div class="progress-track">
              <i></i>
            </div>
            <p>GET /api/meals/daily 연결 필요</p>
          </section>

          <div class="macro-grid pending-api">
            <div>
              <span>단백질</span>
              <strong>-g</strong>
              <i></i>
            </div>
            <div>
              <span>탄수</span>
              <strong>-g</strong>
              <i></i>
            </div>
            <div>
              <span>지방</span>
              <strong>-g</strong>
              <i></i>
            </div>
          </div>

          <section class="today-log">
            <h2>오늘의 기록</h2>
            <div class="api-list-empty">
              식사/운동 기록 목록 API 연결 필요
            </div>
          </section>
        </aside>
      </div>
    </section>
  </main>
</template>
