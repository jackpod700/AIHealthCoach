<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { marked } from "marked";
import { useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import ExerciseProposalCard from "../../components/chat/ExerciseProposalCard.vue";
import MealProposalCard from "../../components/chat/MealProposalCard.vue";
import { useAuthStore } from "../../stores/authStore";
import { useChatStore } from "../../stores/chatStore";
import { useExerciseStore } from "../../stores/exerciseStore";
import { useMealStore } from "../../stores/mealStore";
import { useProfileStore } from "../../stores/profileStore";

const authStore = useAuthStore();
const chatStore = useChatStore();
const exerciseStore = useExerciseStore();
const mealStore = useMealStore();
const profileStore = useProfileStore();
const router = useRouter();
const message = ref("");
const threadRef = ref(null);

const mealTypeMeta = {
  BREAKFAST: { label: "아침", dot: "yellow" },
  LUNCH: { label: "점심", dot: "orange" },
  DINNER: { label: "저녁", dot: "navy" },
  SNACK: { label: "간식", dot: "yellow" },
};

const todayDateKey = computed(() => toDateKey(new Date()));

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

const todayMeals = computed(() => {
  return mealStore.dailyMeal?.meals || [];
});

const hasTodayMeals = computed(() => todayMeals.value.length > 0);

const todayTotals = computed(() => {
  const dailyMeal = mealStore.dailyMeal;

  return {
    calories: toNumber(dailyMeal?.dailyTotalCalories),
    carbohydrate: toNumber(dailyMeal?.dailyTotalCarbohydrate),
    protein: toNumber(dailyMeal?.dailyTotalProtein),
    fat: toNumber(dailyMeal?.dailyTotalFat),
  };
});

marked.setOptions({
  breaks: true,
  gfm: true,
});

onMounted(async () => {
  await Promise.all([
    chatStore.loadMessages(),
    mealStore.loadDailyMeal(todayDateKey.value),
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

  await mealStore.loadDailyMeal(todayDateKey.value);
  await scrollToBottom();
}

async function confirmMealProposal(payload) {
  const response = await chatStore.confirmMealProposal(payload);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (response?.dailyMeal) {
    mealStore.dailyMeal = response.dailyMeal;
  } else {
    await mealStore.loadDailyMeal(todayDateKey.value);
  }

  await scrollToBottom();
}

async function confirmExerciseProposal(payload) {
  if (!chatStore.startConfirmingExercise()) {
    return;
  }

  const saved = await exerciseStore.saveRecord(payload);

  if (!authStore.isAuthenticated) {
    chatStore.finishConfirmingExercise();
    router.replace("/login");
    return;
  }

  if (saved) {
    chatStore.completeExerciseProposal();
  } else {
    chatStore.failExerciseProposal(exerciseStore.saveRecordError || "운동 기록 저장에 실패했습니다.");
  }

  chatStore.finishConfirmingExercise();
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

function mealLabel(mealType) {
  return mealTypeMeta[mealType]?.label || mealType;
}

function mealDot(mealType) {
  return mealTypeMeta[mealType]?.dot || "yellow";
}

function mealFoodNames(meal) {
  return meal.items?.map((item) => item.foodName).join(" + ") || mealLabel(meal.mealType);
}

function toNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatNumber(value) {
  return Math.round(toNumber(value)).toLocaleString("ko-KR");
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
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

            <div v-if="chatStore.mealProposal" class="message-row coach">
              <div class="coach-icon">
                <i class="pi pi-briefcase"></i>
              </div>
              <MealProposalCard
                :proposal="chatStore.mealProposal"
                :is-confirming="chatStore.isConfirmingMeal"
                :error="chatStore.mealProposalError"
                @confirm="confirmMealProposal"
                @dismiss="chatStore.dismissMealProposal"
              />
            </div>

            <div v-if="chatStore.exerciseProposal" class="message-row coach">
              <div class="coach-icon">
                <i class="pi pi-bolt"></i>
              </div>
              <ExerciseProposalCard
                :proposal="chatStore.exerciseProposal"
                :is-confirming="chatStore.isConfirmingExercise"
                :error="chatStore.exerciseProposalError"
                @confirm="confirmExerciseProposal"
                @dismiss="chatStore.dismissExerciseProposal"
              />
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

          <div v-if="mealStore.dailyError" class="api-needed-panel">
            <strong>오늘 식단 정보를 불러오지 못했어요</strong>
            <p>{{ mealStore.dailyError }}</p>
          </div>

          <section class="calorie-card" :class="{ 'pending-api': !hasTodayMeals }">
            <span>오늘 섭취 칼로리</span>
            <strong>
              {{ hasTodayMeals ? formatNumber(todayTotals.calories) : "-" }}<small>/ 목표 kcal API 필요</small>
            </strong>
            <div class="progress-track">
              <i></i>
            </div>
            <p v-if="hasTodayMeals">일일 식단 조회 API 기준으로 계산됐어요</p>
            <p v-else>아직 오늘 식단 기록이 없어요</p>
          </section>

          <div class="macro-grid" :class="{ 'pending-api': !hasTodayMeals }">
            <div>
              <span>단백질</span>
              <strong>{{ hasTodayMeals ? formatNumber(todayTotals.protein) : "-" }}g</strong>
              <i></i>
            </div>
            <div>
              <span>탄수</span>
              <strong>{{ hasTodayMeals ? formatNumber(todayTotals.carbohydrate) : "-" }}g</strong>
              <i></i>
            </div>
            <div>
              <span>지방</span>
              <strong>{{ hasTodayMeals ? formatNumber(todayTotals.fat) : "-" }}g</strong>
              <i></i>
            </div>
          </div>

          <section class="today-log">
            <h2>오늘의 기록</h2>

            <article v-for="meal in todayMeals" :key="meal.mealId">
              <time>{{ mealLabel(meal.mealType) }}</time>
              <i :class="['dot', mealDot(meal.mealType)]"></i>
              <div>
                <strong>{{ mealFoodNames(meal) }}</strong>
                <span>{{ mealLabel(meal.mealType) }} · {{ formatNumber(meal.totalCalories) }} kcal</span>
              </div>
            </article>

            <div v-if="!mealStore.isLoadingDaily && !hasTodayMeals" class="api-list-empty">
              오늘 저장된 식단 기록이 없어요.
            </div>

            <div class="api-list-empty compact">
              운동 기록 API 연결 필요
            </div>
          </section>
        </aside>
      </div>
    </section>
  </main>
</template>
