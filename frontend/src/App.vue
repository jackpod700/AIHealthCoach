<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useHealthStore } from "./stores/healthStore";

const healthStore = useHealthStore();
const message = ref("");
const messagesRef = ref(null);
const loginForm = ref({
  email: "",
  password: "",
});

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

onMounted(async () => {
  if (healthStore.isAuthenticated) {
    await healthStore.loadMessages();
    scrollToBottom();
  }
});

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

async function submitLogin() {
  await healthStore.login({
    email: loginForm.value.email.trim(),
    password: loginForm.value.password,
  });

  if (healthStore.isAuthenticated) {
    loginForm.value.password = "";
    scrollToBottom();
  }
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
          <h1>로그인</h1>
        </div>

        <form
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
          label="이력 새로고침"
          icon="pi pi-refresh"
          severity="contrast"
          :loading="healthStore.isLoading"
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

    <main class="layout">
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
    </template>
  </div>
</template>
