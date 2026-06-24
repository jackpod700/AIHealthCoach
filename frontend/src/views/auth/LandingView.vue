<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";

const router = useRouter();
const authStore = useAuthStore();

const features = [
  {
    icon: "pi pi-comments",
    title: "대화로 끝나는 기록",
    desc: "먹은 음식과 운동을 말하듯 적으면 AI가 알아서 정리하고 분류해요.",
    tone: "green",
  },
  {
    icon: "pi pi-chart-bar",
    title: "자동 영양 분석",
    desc: "칼로리와 탄단지를 자동으로 계산하고 목표 대비 진행률을 보여줘요.",
    tone: "orange",
  },
  {
    icon: "pi pi-bullseye",
    title: "나에게 맞는 코칭",
    desc: "목표 강도에 맞춰 매일의 코칭과 다음 행동을 추천받을 수 있어요.",
    tone: "green",
  },
];

function goLogin() {
  router.push(authStore.isAuthenticated ? "/chat" : "/login");
}

function goSignup() {
  router.push(authStore.isAuthenticated ? "/chat" : "/signup");
}
</script>

<template>
  <main class="landing-screen">
    <header class="landing-nav">
      <button type="button" class="auth-brand-link" @click="router.push('/')">
        <span class="brand-mark"><i class="pi pi-briefcase"></i></span>
        <span>
          <strong>BabStroy</strong>
        </span>
      </button>
    </header>

    <section class="landing-hero">
      <div class="landing-hero-copy">
        <div class="landing-kicker">
          <span></span>
          BabStroy
        </div>
        <h1>
          기록만 하세요.<br />
          코칭은 <b>AI가</b> 합니다.
        </h1>
        <p>
          먹은 음식과 운동을 말하듯 적기만 하면,<br />
          AI 헬스 코치 얌냠이가 영양을 분석하고 매일의 목표를 함께 관리해줘요.
        </p>
        <div class="landing-hero-actions">
          <button type="button" class="landing-primary" @click="goLogin">
            사용해보러 가기
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>
      </div>

      <div class="landing-preview" aria-label="오늘의 코칭 미리보기">
        <div class="landing-preview-card">
          <div class="landing-preview-head">
            <div>
              <span>TODAY'S COACHING</span>
              <strong>오늘의 코칭</strong>
            </div>
            <em><i></i> 유지 목표 진행 중</em>
          </div>

          <div class="landing-progress">
            <span>오늘 섭취 목표</span>
            <strong>584 <small>/ 2,100 kcal</small></strong>
            <div><b></b></div>
          </div>

          <div class="landing-coach-message">
            <span><i class="pi pi-briefcase"></i></span>
            <p>걷기 외에도 자전거 타기를 추천해요. 하체 근력에 특히 좋아요!</p>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-features">
      <div class="landing-section-head">
        <span>WHY HEALTH COACH</span>
        <h2>기록은 가볍게, 결과는 정확하게</h2>
      </div>

      <div class="landing-feature-grid">
        <article
          v-for="feature in features"
          :key="feature.title"
          class="landing-feature-card"
        >
          <span :class="['landing-feature-icon', feature.tone]">
            <i :class="feature.icon"></i>
          </span>
          <strong>{{ feature.title }}</strong>
          <p>{{ feature.desc }}</p>
        </article>
      </div>
    </section>
  </main>
</template>
