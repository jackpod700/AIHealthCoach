<script setup>
import { computed, ref } from "vue";
import { useHealthStore } from "./stores/healthStore";

const healthStore = useHealthStore();
const message = ref("");

const caloriePercent = computed(() => Math.round((healthStore.summary.calories / healthStore.summary.calorieGoal) * 100));
const proteinPercent = computed(() => Math.round((healthStore.summary.protein / healthStore.summary.proteinGoal) * 100));
const waterPercent = computed(() => Math.round((healthStore.summary.water / healthStore.summary.waterGoal) * 100));

function sendMessage() {
  if (!message.value.trim()) {
    return;
  }

  healthStore.addMessage(message.value.trim());
  message.value = "";
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">AI Health Coach</p>
        <h1>대화로 기록하는 건강 코치</h1>
      </div>
      <Button label="오늘 요약" icon="pi pi-chart-line" severity="contrast" />
    </header>

    <main class="layout">
      <section class="chat-panel">
        <Card>
          <template #title>
            <div class="section-title">
              <i class="pi pi-comments"></i>
              AI 기록 코치
            </div>
          </template>
          <template #content>
            <div class="messages">
              <div
                v-for="chat in healthStore.messages"
                :key="chat.id"
                class="message"
                :class="chat.role"
              >
                <span>{{ chat.text }}</span>
              </div>
            </div>

            <div class="quick-prompts">
              <Chip
                v-for="prompt in healthStore.quickPrompts"
                :key="prompt"
                :label="prompt"
                @click="message = prompt"
              />
            </div>

            <form class="chat-input" @submit.prevent="sendMessage">
              <InputText
                v-model="message"
                class="chat-text"
                placeholder="예: 점심에 닭가슴살 샐러드 먹었고 20분 걸었어"
              />
              <Button type="submit" icon="pi pi-send" aria-label="보내기" />
            </form>
          </template>
        </Card>
      </section>

      <section class="workspace">
        <Card class="summary-card">
          <template #title>오늘 건강 요약</template>
          <template #content>
            <div class="score-row">
              <div>
                <span class="score">{{ healthStore.summary.score }}</span>
                <span class="score-total">/ 100</span>
              </div>
              <Tag value="균형 양호" severity="success" />
            </div>

            <div class="metric-list">
              <div class="metric">
                <div class="metric-head">
                  <span>칼로리</span>
                  <strong>{{ healthStore.summary.calories }} / {{ healthStore.summary.calorieGoal }} kcal</strong>
                </div>
                <ProgressBar :value="caloriePercent" />
              </div>
              <div class="metric">
                <div class="metric-head">
                  <span>단백질</span>
                  <strong>{{ healthStore.summary.protein }} / {{ healthStore.summary.proteinGoal }} g</strong>
                </div>
                <ProgressBar :value="proteinPercent" />
              </div>
              <div class="metric">
                <div class="metric-head">
                  <span>수분</span>
                  <strong>{{ healthStore.summary.water }} / {{ healthStore.summary.waterGoal }} L</strong>
                </div>
                <ProgressBar :value="waterPercent" />
              </div>
            </div>
          </template>
        </Card>

        <Card>
          <template #title>AI가 인식한 기록</template>
          <template #content>
            <div class="record-grid">
              <article
                v-for="record in healthStore.records"
                :key="record.id"
                class="record-card"
              >
                <div class="record-head">
                  <Tag :value="record.type" :severity="record.severity" />
                  <span>{{ record.time }}</span>
                </div>
                <h3>{{ record.title }}</h3>
                <p>{{ record.description }}</p>
                <strong>{{ record.value }}</strong>
              </article>
            </div>
          </template>
        </Card>
      </section>
    </main>
  </div>
</template>
