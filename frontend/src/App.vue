<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useHealthStore } from "./stores/healthStore";

const healthStore = useHealthStore();
const message = ref("");
const messagesRef = ref(null);
const loginForm = ref({
  email: "",
  password: "",
});
const activeView = ref("chat");
const authMode = ref("login");
const foodSearchDebounceTimer = ref(null);
const signupForm = ref({
  email: "",
  password: "",
  nickname: "",
});
const profileForm = ref({
  heightCm: "",
  currentWeightKg: "",
  targetWeightKg: "",
  goalType: "",
});

const goalOptions = [
  { value: "WEIGHT_LOSS", label: "감량" },
  { value: "MAINTAIN", label: "유지" },
  { value: "MUSCLE_GAIN", label: "근육 증가" },
];

const mealTypeLabels = {
  BREAKFAST: "아침",
  LUNCH: "점심",
  DINNER: "저녁",
  SNACK: "간식",
};

const mealTypeOptions = Object.entries(mealTypeLabels).map(([value, label]) => ({ value, label }));

const displayMessages = computed(() => {
  if (healthStore.orderedMessages.length > 0) {
    return healthStore.orderedMessages;
  }

  return [
    {
      role: "ASSISTANT",
      content: "먹은 음식, 운동, 수분 섭취를 자연스럽게 입력하면 기록하고 답변할게요.",
      createdAt: new Date().toISOString(),
    },
  ];
});

const latestAssistantText = computed(() => {
  return healthStore.lastAssistantMessage?.content || "아직 AI 응답이 없습니다. 오늘의 기록을 한 문장으로 남겨보세요.";
});

const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

const calendarDays = computed(() => {
  const [year, month] = healthStore.selectedCalendarMonth.split("-").map(Number);
  const firstDate = new Date(year, month - 1, 1);
  const daysInMonth = new Date(year, month, 0).getDate();
  const leadingBlankCount = firstDate.getDay();
  const cellCount = Math.ceil((leadingBlankCount + daysInMonth) / 7) * 7;
  const summariesByDate = new Map((healthStore.mealCalendar?.days || []).map((day) => [day.date, day]));

  return Array.from({ length: cellCount }, (_, index) => {
    const dayNumber = index - leadingBlankCount + 1;
    if (dayNumber < 1 || dayNumber > daysInMonth) {
      return {
        key: `blank-${index}`,
        inMonth: false,
      };
    }

    const date = `${year}-${String(month).padStart(2, "0")}-${String(dayNumber).padStart(2, "0")}`;
    return {
      key: date,
      date,
      dayNumber,
      inMonth: true,
      summary: summariesByDate.get(date) || null,
    };
  });
});

onMounted(async () => {
  if (healthStore.isAuthenticated) {
    await Promise.all([
      healthStore.loadMessages(),
      healthStore.loadProfile(),
    ]);
    fillProfileForm();
    scrollToBottom();
  }
});

watch(
  () => healthStore.profile,
  () => fillProfileForm(),
);

watch(
  () => healthStore.manualFoodQuery,
  (query) => {
    if (foodSearchDebounceTimer.value) {
      clearTimeout(foodSearchDebounceTimer.value);
    }

    foodSearchDebounceTimer.value = setTimeout(() => {
      healthStore.searchManualFoods(query);
    }, 250);
  },
);

function formatTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function roleLabel(role) {
  return role === "USER" ? "나" : "AI 코치";
}

function roleSeverity(role) {
  return role === "USER" ? "info" : "success";
}

function renderMarkdown(content = "") {
  const escaped = escapeHtml(content);
  const lines = escaped.split(/\r?\n/);
  const blocks = [];
  let listItems = [];
  let codeLines = [];
  let tableRows = [];
  let inCodeBlock = false;

  function flushList() {
    if (listItems.length > 0) {
      blocks.push(`<ul>${listItems.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join("")}</ul>`);
      listItems = [];
    }
  }

  function flushCode() {
    if (codeLines.length > 0) {
      blocks.push(`<pre><code>${codeLines.join("\n")}</code></pre>`);
      codeLines = [];
    }
  }

  function flushTable() {
    if (tableRows.length > 0) {
      blocks.push(renderTable(tableRows));
      tableRows = [];
    }
  }

  lines.forEach((line) => {
    if (line.trim().startsWith("```")) {
      if (inCodeBlock) {
        flushCode();
      } else {
        flushList();
        flushTable();
      }

      inCodeBlock = !inCodeBlock;
      return;
    }

    if (inCodeBlock) {
      codeLines.push(line);
      return;
    }

    const listMatch = line.match(/^\s*[-*]\s+(.+)$/);
    const isTableRow = /^\s*\|.+\|\s*$/.test(line);

    if (listMatch) {
      flushTable();
      listItems.push(listMatch[1]);
      return;
    }

    if (isTableRow) {
      flushList();
      tableRows.push(line);
      return;
    }

    flushList();
    flushTable();

    if (!line.trim()) {
      blocks.push("<br>");
      return;
    }

    if (line.startsWith("### ")) {
      blocks.push(`<h3>${renderInlineMarkdown(line.slice(4))}</h3>`);
      return;
    }

    if (line.startsWith("## ")) {
      blocks.push(`<h2>${renderInlineMarkdown(line.slice(3))}</h2>`);
      return;
    }

    blocks.push(`<p>${renderInlineMarkdown(line)}</p>`);
  });

  flushList();
  flushTable();
  flushCode();

  return blocks.join("");
}

function renderTable(rows = []) {
  const parsedRows = rows
    .map((row) => row.trim().replace(/^\||\|$/g, "").split("|").map((cell) => cell.trim()))
    .filter((cells) => cells.length > 0);

  if (parsedRows.length === 0) {
    return "";
  }

  const [head, maybeDivider, ...bodyRows] = parsedRows;
  const hasDivider = maybeDivider?.every((cell) => /^:?-{3,}:?$/.test(cell));
  const rowsToRender = hasDivider ? bodyRows : parsedRows.slice(1);

  return `
    <div class="markdown-table-wrap">
      <table>
        <thead>
          <tr>${head.map((cell) => `<th>${renderInlineMarkdown(cell)}</th>`).join("")}</tr>
        </thead>
        <tbody>
          ${rowsToRender.map((row) => `<tr>${row.map((cell) => `<td>${renderInlineMarkdown(cell)}</td>`).join("")}</tr>`).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderInlineMarkdown(content = "") {
  return content
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>");
}

function escapeHtml(content = "") {
  return content
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

async function sendMessage() {
  if (!message.value.trim()) {
    return;
  }

  const content = message.value;
  message.value = "";
  await healthStore.sendMessage(content);
  scrollToBottom();
}

async function confirmMealProposal() {
  await healthStore.confirmMealProposal();
  scrollToBottom();
}

async function submitLogin() {
  await healthStore.login({
    email: loginForm.value.email.trim(),
    password: loginForm.value.password,
  });

  if (healthStore.isAuthenticated) {
    loginForm.value.password = "";
    fillProfileForm();
    scrollToBottom();
  }
}

async function submitSignup() {
  await healthStore.signup({
    email: signupForm.value.email.trim(),
    password: signupForm.value.password,
    nickname: signupForm.value.nickname.trim(),
  });

  if (healthStore.isAuthenticated) {
    signupForm.value.password = "";
    fillProfileForm();
    scrollToBottom();
  }
}

async function submitProfile() {
  await healthStore.updateProfile({
    heightCm: toNullableNumber(profileForm.value.heightCm),
    currentWeightKg: toNullableNumber(profileForm.value.currentWeightKg),
    targetWeightKg: toNullableNumber(profileForm.value.targetWeightKg),
    goalType: profileForm.value.goalType || null,
  });
}

function fillProfileForm() {
  const profile = healthStore.profile;

  if (!profile) {
    profileForm.value = {
      heightCm: "",
      currentWeightKg: "",
      targetWeightKg: "",
      goalType: "",
    };
    return;
  }

  profileForm.value = {
    heightCm: profile.heightCm ?? "",
    currentWeightKg: profile.currentWeightKg ?? "",
    targetWeightKg: profile.targetWeightKg ?? "",
    goalType: profile.goalType ?? "",
  };
}

function toNullableNumber(value) {
  if (value === "" || value === null || value === undefined) {
    return null;
  }

  return Number(value);
}

function goalLabel(value) {
  return goalOptions.find((option) => option.value === value)?.label || "미설정";
}

function mealTypeLabel(value) {
  return mealTypeLabels[value] || value;
}

function formatNumber(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return Number(value).toLocaleString("ko-KR", {
    maximumFractionDigits: 2,
  });
}

function selectedCandidate(item, index) {
  const selectedFoodCode = healthStore.mealProposalSelections[index];
  return item.candidates?.find((candidate) => candidate.foodCode === selectedFoodCode) || null;
}

function calendarMonthTitle() {
  const [year, month] = healthStore.selectedCalendarMonth.split("-").map(Number);
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
  }).format(new Date(year, month - 1, 1));
}

async function openCalendarView() {
  activeView.value = "calendar";

  if (!healthStore.mealCalendar) {
    const [year, month] = healthStore.selectedCalendarMonth.split("-").map(Number);
    await healthStore.loadMonthlyMeals(year, month);
  }
}

async function moveCalendarMonth(offset) {
  const [year, month] = healthStore.selectedCalendarMonth.split("-").map(Number);
  const nextMonth = new Date(year, month - 1 + offset, 1);
  await healthStore.loadMonthlyMeals(nextMonth.getFullYear(), nextMonth.getMonth() + 1);
}

async function selectCalendarDate(day) {
  if (!day.inMonth) {
    return;
  }

  await healthStore.loadDailyMeal(day.date);
}

function openManualMealForm(meal = null) {
  const date = healthStore.selectedMealDate || new Date().toISOString().slice(0, 10);
  healthStore.openManualMealForm(date, meal);
}

async function scrollToBottom() {
  await nextTick();

  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
  }
}
</script>

<template>
  <div class="app-shell">
    <main
      v-if="!healthStore.isAuthenticated"
      class="login-layout"
    >
      <section class="login-panel">
        <div>
          <p class="eyebrow">AI Health Coach</p>
          <h1>{{ authMode === "login" ? "로그인" : "회원가입" }}</h1>
        </div>

        <form
          v-if="authMode === 'login'"
          class="login-form"
          @submit.prevent="submitLogin"
        >
          <label>
            <span>이메일</span>
            <input
              v-model="loginForm.email"
              type="email"
              autocomplete="email"
              placeholder="test@example.com"
              required
            />
          </label>

          <label>
            <span>비밀번호</span>
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="비밀번호"
              required
            />
          </label>

          <p
            v-if="healthStore.loginError"
            class="error-banner"
          >
            {{ healthStore.loginError }}
          </p>

          <Button
            type="submit"
            label="로그인"
            icon="pi pi-sign-in"
            :loading="healthStore.isLoggingIn"
          />
        </form>

        <form
          v-else
          class="login-form"
          @submit.prevent="submitSignup"
        >
          <label>
            <span>닉네임</span>
            <input
              v-model="signupForm.nickname"
              type="text"
              autocomplete="nickname"
              placeholder="닉네임"
              required
            />
          </label>

          <label>
            <span>이메일</span>
            <input
              v-model="signupForm.email"
              type="email"
              autocomplete="email"
              placeholder="test@example.com"
              required
            />
          </label>

          <label>
            <span>비밀번호</span>
            <input
              v-model="signupForm.password"
              type="password"
              autocomplete="new-password"
              placeholder="비밀번호"
              required
            />
          </label>

          <p
            v-if="healthStore.signupError"
            class="error-banner"
          >
            {{ healthStore.signupError }}
          </p>

          <Button
            type="submit"
            label="회원가입"
            icon="pi pi-user-plus"
            :loading="healthStore.isSigningUp || healthStore.isLoggingIn"
          />
        </form>

        <button
          class="auth-switch"
          type="button"
          @click="authMode = authMode === 'login' ? 'signup' : 'login'"
        >
          {{ authMode === "login" ? "계정이 없으면 회원가입" : "이미 계정이 있으면 로그인" }}
        </button>
      </section>
    </main>

    <template v-else>
    <header class="topbar">
      <div>
        <p class="eyebrow">AI Health Coach</p>
        <h1>AI 챗봇 건강 기록</h1>
      </div>
      <div class="topbar-actions">
        <span class="user-chip">{{ healthStore.user?.nickname || healthStore.user?.email }}</span>
        <Button
          label="채팅"
          icon="pi pi-comments"
          :severity="activeView === 'chat' ? 'success' : 'secondary'"
          :outlined="activeView !== 'chat'"
          @click="activeView = 'chat'"
        />
        <Button
          label="프로필"
          icon="pi pi-id-card"
          :severity="activeView === 'profile' ? 'success' : 'secondary'"
          :outlined="activeView !== 'profile'"
          @click="activeView = 'profile'"
        />
        <Button
          label="캘린더"
          icon="pi pi-calendar"
          :severity="activeView === 'calendar' ? 'success' : 'secondary'"
          :outlined="activeView !== 'calendar'"
          @click="openCalendarView"
        />
        <Button
          label="이력 새로고침"
          icon="pi pi-refresh"
          severity="contrast"
          :loading="healthStore.isLoading"
          v-if="activeView === 'chat'"
          @click="healthStore.loadMessages"
        />
        <Button
          label="로그아웃"
          icon="pi pi-sign-out"
          severity="secondary"
          outlined
          @click="healthStore.logout"
        />
      </div>
    </header>

    <main
      v-if="activeView === 'chat'"
      class="layout"
    >
      <section class="chat-panel">
        <div class="chat-surface">
          <div class="section-title">
            <i class="pi pi-comments"></i>
            <span>채팅 기록</span>
            <Tag :value="`User #${healthStore.userId}`" severity="secondary" />
          </div>

          <div
            ref="messagesRef"
            class="messages"
          >
            <div
              v-for="(chat, index) in displayMessages"
              :key="chat.clientId || `${chat.role}-${chat.createdAt}-${index}`"
              class="message-row"
              :class="{ mine: chat.role === 'USER' }"
            >
              <div
                class="message"
                :class="[
                  chat.role === 'USER' ? 'user' : 'assistant',
                  { pending: chat.pending, failed: chat.failed },
                ]"
              >
                <div class="message-meta">
                  <Tag
                    :value="roleLabel(chat.role)"
                    :severity="roleSeverity(chat.role)"
                  />
                  <i
                    v-if="chat.pending && !chat.failed"
                    class="pi pi-spin pi-spinner"
                    aria-hidden="true"
                  ></i>
                  <time>{{ formatTime(chat.createdAt) }}</time>
                </div>
                <div
                  v-if="chat.role === 'ASSISTANT'"
                  class="markdown-body"
                  v-html="renderMarkdown(chat.content)"
                ></div>
                <p v-else>{{ chat.content }}</p>
              </div>
            </div>
          </div>

          <section
            v-if="healthStore.mealProposal"
            class="meal-proposal"
          >
            <div class="section-title">
              <i class="pi pi-check-circle"></i>
              <span>식단 기록 확인</span>
              <Tag
                :value="mealTypeLabel(healthStore.mealProposal.mealType)"
                severity="success"
              />
            </div>

            <div class="meal-proposal-meta">
              <span>{{ healthStore.mealProposal.mealDate }}</span>
              <span
                v-if="healthStore.mealProposal.defaultsApplied?.length"
              >
                기본값 적용: {{ healthStore.mealProposal.defaultsApplied.join(", ") }}
              </span>
            </div>

            <div class="meal-proposal-items">
              <article
                v-for="(item, index) in healthStore.mealProposal.items"
                :key="`${item.extractedName}-${index}`"
                class="meal-proposal-item"
              >
                <div>
                  <strong>{{ item.extractedName }}</strong>
                  <span
                    v-if="selectedCandidate(item, index)"
                  >
                    기준량 {{ formatNumber(selectedCandidate(item, index).servingSize) }}
                    {{ selectedCandidate(item, index).servingUnit }}
                  </span>
                  <span v-else>후보를 선택하면 기준량이 표시됩니다.</span>
                </div>

                <div class="meal-proposal-controls">
                  <select
                    v-if="item.candidates?.length"
                    :value="healthStore.mealProposalSelections[index]"
                    @change="healthStore.selectMealCandidate(index, $event.target.value)"
                  >
                    <option value="">음식 후보 선택</option>
                    <option
                      v-for="candidate in item.candidates"
                      :key="candidate.foodCode"
                      :value="candidate.foodCode"
                    >
                      {{ candidate.foodName }} · {{ formatNumber(candidate.calories) }} kcal
                    </option>
                  </select>

                  <label
                    v-if="item.candidates?.length"
                    class="quantity-control"
                  >
                    <span>배수</span>
                    <input
                      type="number"
                      min="0.1"
                      step="0.1"
                      :value="healthStore.mealProposalQuantities[index]"
                      @input="healthStore.updateMealProposalQuantity(index, $event.target.value)"
                    />
                  </label>

                  <p
                    v-else
                    class="proposal-warning"
                  >
                    매칭된 음식 후보가 없습니다.
                  </p>
                </div>
              </article>
            </div>

            <div class="meal-proposal-actions">
              <Button
                type="button"
                label="기록하기"
                icon="pi pi-save"
                :disabled="!healthStore.canConfirmMealProposal"
                :loading="healthStore.isConfirmingMealProposal"
                @click="confirmMealProposal"
              />
              <Button
                type="button"
                label="취소"
                icon="pi pi-times"
                severity="secondary"
                outlined
                @click="healthStore.cancelMealProposal"
              />
            </div>
          </section>

          <p
            v-if="healthStore.error"
            class="error-banner"
          >
            {{ healthStore.error }}
          </p>

          <div class="quick-prompts">
            <Chip
              v-for="prompt in healthStore.quickPrompts"
              :key="prompt"
              :label="prompt"
              @click="message = prompt"
            />
          </div>

          <form
            class="chat-input"
            @submit.prevent="sendMessage"
          >
            <textarea
              v-model="message"
              class="chat-text"
              rows="3"
              placeholder="예: 점심에 닭가슴살 샐러드 먹었고 20분 걸었어"
            ></textarea>
            <Button
              type="submit"
              icon="pi pi-send"
              label="전송"
              :loading="healthStore.isSending"
            />
          </form>
        </div>
      </section>

      <section class="workspace">
        <section class="summary-band">
          <div class="summary-item">
            <span>저장된 메시지</span>
            <strong>{{ healthStore.messages.length }}</strong>
          </div>
          <div class="summary-item">
            <span>식단 기록 후보</span>
            <strong>{{ healthStore.summary.mealCount }}</strong>
          </div>
          <div class="summary-item">
            <span>운동 기록 후보</span>
            <strong>{{ healthStore.summary.exerciseCount }}</strong>
          </div>
          <div class="summary-item">
            <span>AI 응답</span>
            <strong>{{ healthStore.summary.assistantCount }}</strong>
          </div>
        </section>

        <section class="coach-panel">
          <div class="section-title">
            <i class="pi pi-sparkles"></i>
            <span>최근 AI 코치 응답</span>
          </div>
          <div
            class="markdown-body"
            v-html="renderMarkdown(latestAssistantText)"
          ></div>
        </section>

        <section class="api-panel">
          <div class="section-title">
            <i class="pi pi-server"></i>
            <span>연동된 백엔드 API</span>
          </div>
          <div class="api-list">
            <article>
              <div>
                <Tag value="GET" severity="info" />
                <strong>/api/chat/messages</strong>
              </div>
              <p>사용자의 이전 채팅 메시지를 불러옵니다.</p>
            </article>
            <article>
              <div>
                <Tag value="POST" severity="success" />
                <strong>/api/chat/messages</strong>
              </div>
              <p>사용자 메시지를 저장하고 AI 응답을 생성해 함께 저장합니다.</p>
            </article>
          </div>
        </section>
      </section>
    </main>

    <main
      v-else-if="activeView === 'calendar'"
      class="calendar-page"
    >
      <section class="calendar-panel">
        <div class="calendar-toolbar">
          <div>
            <p class="eyebrow">Meal Calendar</p>
            <h2>{{ calendarMonthTitle() }}</h2>
          </div>
          <div class="calendar-actions">
            <Button
              type="button"
              icon="pi pi-chevron-left"
              severity="secondary"
              outlined
              @click="moveCalendarMonth(-1)"
            />
            <Button
              type="button"
              icon="pi pi-refresh"
              severity="secondary"
              outlined
              :loading="healthStore.isLoadingMealCalendar"
              @click="moveCalendarMonth(0)"
            />
            <Button
              type="button"
              icon="pi pi-chevron-right"
              severity="secondary"
              outlined
              @click="moveCalendarMonth(1)"
            />
          </div>
        </div>

        <div class="calendar-grid">
          <div
            v-for="dayName in weekDays"
            :key="dayName"
            class="calendar-weekday"
          >
            {{ dayName }}
          </div>
          <button
            v-for="day in calendarDays"
            :key="day.key"
            type="button"
            class="calendar-day"
            :class="{
              blank: !day.inMonth,
              selected: day.date === healthStore.selectedMealDate,
              recorded: day.summary,
            }"
            :disabled="!day.inMonth"
            @click="selectCalendarDate(day)"
          >
            <span class="calendar-day-number">{{ day.dayNumber }}</span>
            <strong v-if="day.summary">{{ formatNumber(day.summary.totalCalories) }} kcal</strong>
            <span
              v-if="day.summary"
              class="calendar-meal-types"
            >
              {{ day.summary.mealTypes.map(mealTypeLabel).join(" · ") }}
            </span>
          </button>
        </div>

        <p
          v-if="healthStore.mealCalendarError"
          class="error-banner"
        >
          {{ healthStore.mealCalendarError }}
        </p>
      </section>

      <section class="daily-meal-panel">
        <div class="section-title">
          <i class="pi pi-list"></i>
          <span>선택한 날짜 상세</span>
          <Tag
            v-if="healthStore.selectedMealDate"
            :value="healthStore.selectedMealDate"
            severity="secondary"
          />
          <Button
            v-if="healthStore.selectedMealDate"
            type="button"
            label="식단 작성"
            icon="pi pi-plus"
            size="small"
            @click="openManualMealForm()"
          />
        </div>

        <form
          v-if="healthStore.manualMealForm"
          class="manual-meal-form"
          @submit.prevent="healthStore.saveManualMeal"
        >
          <div class="manual-meal-form-row">
            <label>
              <span>날짜</span>
              <input
                :value="healthStore.manualMealForm.mealDate"
                type="text"
                readonly
              />
            </label>
            <label>
              <span>끼니</span>
              <select v-model="healthStore.manualMealForm.mealType">
                <option
                  v-for="option in mealTypeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <div class="food-search-box">
            <label>
              <span>음식 검색</span>
              <input
                v-model="healthStore.manualFoodQuery"
                type="search"
                placeholder="예: 닭가슴살"
              />
            </label>
            <i
              v-if="healthStore.isSearchingFoods"
              class="pi pi-spin pi-spinner food-search-spinner"
              aria-hidden="true"
            ></i>
          </div>

          <div
            v-if="healthStore.manualFoodResults.length"
            class="food-search-results"
          >
            <button
              v-for="food in healthStore.manualFoodResults"
              :key="food.foodCode"
              type="button"
              @click="healthStore.addManualMealFood(food)"
            >
              <span>
                <strong>{{ food.foodName }}</strong>
                {{ food.manufacturer || "제조사 없음" }}
              </span>
              <small>
                기준량 {{ formatNumber(food.servingSize) }} {{ food.servingUnit }}
                · {{ formatNumber(food.calories) }} kcal
              </small>
            </button>
          </div>

          <div class="manual-meal-items">
            <article
              v-for="(item, index) in healthStore.manualMealForm.items"
              :key="item.foodCode"
              class="manual-meal-item"
            >
              <div>
                <strong>{{ item.foodName }}</strong>
                <span>
                  기준량 {{ formatNumber(item.servingSize) }} {{ item.servingUnit }}
                  · {{ formatNumber(item.calories) }} kcal
                </span>
              </div>
              <label>
                <span>배수</span>
                <input
                  type="number"
                  min="0.1"
                  step="0.1"
                  :value="item.quantity"
                  @input="healthStore.updateManualMealQuantity(index, $event.target.value)"
                  @change="healthStore.normalizeManualMealQuantity(index)"
                />
              </label>
              <Button
                type="button"
                icon="pi pi-times"
                severity="secondary"
                outlined
                @click="healthStore.removeManualMealFood(index)"
              />
            </article>
          </div>

          <p
            v-if="!healthStore.manualMealForm.items.length"
            class="empty-state"
          >
            음식을 검색해서 식단에 추가하세요.
          </p>

          <div class="manual-meal-actions">
            <Button
              type="submit"
              label="저장"
              icon="pi pi-save"
              :disabled="!healthStore.canSaveManualMeal"
              :loading="healthStore.isSavingManualMeal"
            />
            <Button
              type="button"
              label="취소"
              icon="pi pi-times"
              severity="secondary"
              outlined
              @click="healthStore.cancelManualMealForm"
            />
          </div>
        </form>

        <template v-if="healthStore.isLoadingDailyMeal">
          <p class="empty-state">식단 상세를 불러오는 중입니다.</p>
        </template>

        <template v-else-if="healthStore.selectedDailyMeal?.meals?.length">
          <div class="daily-totals">
            <article>
              <span>총 칼로리</span>
              <strong>{{ formatNumber(healthStore.selectedDailyMeal.dailyTotalCalories) }} kcal</strong>
            </article>
            <article>
              <span>탄수화물</span>
              <strong>{{ formatNumber(healthStore.selectedDailyMeal.dailyTotalCarbohydrate) }} g</strong>
            </article>
            <article>
              <span>단백질</span>
              <strong>{{ formatNumber(healthStore.selectedDailyMeal.dailyTotalProtein) }} g</strong>
            </article>
            <article>
              <span>지방</span>
              <strong>{{ formatNumber(healthStore.selectedDailyMeal.dailyTotalFat) }} g</strong>
            </article>
          </div>

          <div class="daily-meal-list">
            <article
              v-for="meal in healthStore.selectedDailyMeal.meals"
              :key="meal.mealId"
              class="daily-meal-card"
            >
              <div class="daily-meal-card-header">
                <Tag
                  :value="mealTypeLabel(meal.mealType)"
                  severity="success"
                />
                <div class="daily-meal-card-actions">
                  <strong>{{ formatNumber(meal.totalCalories) }} kcal</strong>
                  <Button
                    type="button"
                    label="수정"
                    icon="pi pi-pencil"
                    size="small"
                    severity="secondary"
                    outlined
                    @click="openManualMealForm(meal)"
                  />
                </div>
              </div>
              <ul>
                <li
                  v-for="item in meal.items"
                  :key="`${meal.mealId}-${item.foodCode}`"
                >
                  <span>{{ item.foodName }}</span>
                  <strong>{{ formatNumber(item.quantity) }}배 · {{ formatNumber(item.calories) }} kcal</strong>
                </li>
              </ul>
            </article>
          </div>
        </template>

        <p
          v-else
          class="empty-state"
        >
          날짜를 선택하면 식단 상세가 표시됩니다.
        </p>
      </section>
    </main>

    <main
      v-else-if="activeView === 'profile'"
      class="profile-page"
    >
      <section class="profile-overview">
        <div>
          <p class="eyebrow">Profile</p>
          <h2>건강 코칭 프로필</h2>
        </div>
        <div class="profile-summary-grid">
          <article>
            <span>키</span>
            <strong>{{ healthStore.profile?.heightCm ?? "-" }} cm</strong>
          </article>
          <article>
            <span>현재 몸무게</span>
            <strong>{{ healthStore.profile?.currentWeightKg ?? "-" }} kg</strong>
          </article>
          <article>
            <span>목표 몸무게</span>
            <strong>{{ healthStore.profile?.targetWeightKg ?? "-" }} kg</strong>
          </article>
          <article>
            <span>목표</span>
            <strong>{{ goalLabel(healthStore.profile?.goalType) }}</strong>
          </article>
        </div>
      </section>

      <section class="profile-editor">
        <div class="section-title">
          <i class="pi pi-pencil"></i>
          <span>프로필 수정</span>
        </div>

        <form
          class="profile-form"
          @submit.prevent="submitProfile"
        >
          <label>
            <span>키(cm)</span>
            <input
              v-model="profileForm.heightCm"
              type="number"
              min="0"
              step="0.1"
              placeholder="172.5"
            />
          </label>

          <label>
            <span>현재 몸무게(kg)</span>
            <input
              v-model="profileForm.currentWeightKg"
              type="number"
              min="0"
              step="0.1"
              placeholder="68.4"
            />
          </label>

          <label>
            <span>목표 몸무게(kg)</span>
            <input
              v-model="profileForm.targetWeightKg"
              type="number"
              min="0"
              step="0.1"
              placeholder="65.0"
            />
          </label>

          <label>
            <span>목표</span>
            <select v-model="profileForm.goalType">
              <option value="">선택 안 함</option>
              <option
                v-for="option in goalOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </label>

          <p
            v-if="healthStore.profileError"
            class="error-banner"
          >
            {{ healthStore.profileError }}
          </p>

          <p
            v-if="healthStore.profileSuccess"
            class="success-banner"
          >
            {{ healthStore.profileSuccess }}
          </p>

          <div class="profile-actions">
            <Button
              type="submit"
              label="프로필 저장"
              icon="pi pi-save"
              :loading="healthStore.isSavingProfile"
            />
            <Button
              type="button"
              label="다시 불러오기"
              icon="pi pi-refresh"
              severity="secondary"
              outlined
              :loading="healthStore.isLoadingProfile"
              @click="healthStore.loadProfile"
            />
          </div>
        </form>
      </section>
    </main>
    </template>
  </div>
</template>
