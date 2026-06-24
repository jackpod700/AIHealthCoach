<script setup>
import { computed, reactive } from "vue";
import { useRouter } from "vue-router";

import FormField from "../../components/auth/FormField.vue";
import { getOAuthLoginUrl } from "../../api/authApi";
import { useAuthStore } from "../../stores/authStore";

const router = useRouter();
const authStore = useAuthStore();

const loginForm = reactive({
  email: "",
  password: "",
});

const loginButtonLabel = computed(() => {
  return authStore.isLoggingIn ? "로그인 중..." : "로그인";
});

async function submitLogin() {
  if (authStore.isLoggingIn) {
    return;
  }

  await authStore.login({
    email: loginForm.email,
    password: loginForm.password,
  });

  if (authStore.isAuthenticated) {
    router.push("/chat");
  }
}

function startOAuthLogin(provider) {
  window.location.href = getOAuthLoginUrl(provider);
}
</script>

<template>
  <main class="login-screen">
    <header class="auth-topbar">
      <button type="button" class="auth-brand-link" @click="router.push('/')">
        <span class="brand-mark"><i class="pi pi-briefcase"></i></span>
        <span>
          <strong>BabStroy</strong>
        </span>
      </button>
      <button type="button" class="auth-close-button" aria-label="랜딩으로 돌아가기" @click="router.push('/')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <section class="login-panel auth-panel" aria-label="로그인">
      <div class="login-card">
        <p class="deco">Sign In</p>

        <h2>다시 오신 걸 환영해요</h2>

        <p class="login-lead">계정에 로그인하고 오늘의 코칭을 받아보세요.</p>

        <form class="login-form" @submit.prevent="submitLogin">
          <FormField
            v-model="loginForm.email"
            label="이메일"
            icon="pi pi-envelope"
            type="email"
            autocomplete="email"
            placeholder="이메일을 입력하세요"
          />

          <FormField
            v-model="loginForm.password"
            label="비밀번호"
            icon="pi pi-lock"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호를 입력하세요"
            show-action-icon
          />

          <div class="login-options">
            <button type="button">비밀번호 찾기</button>
          </div>

          <div v-if="authStore.loginError" class="login-error">
            {{ authStore.loginError }}
          </div>

          <button class="login-submit" type="submit" :disabled="authStore.isLoggingIn">
            {{ loginButtonLabel }}
          </button>
        </form>

        <div class="login-divider">
          <span></span>
          <b>또는</b>
          <span></span>
        </div>

        <div class="social-actions">
          <button type="button" @click="startOAuthLogin('google')">
            <i class="pi pi-google"></i>
            Google로 계속하기
          </button>

          <button type="button" @click="startOAuthLogin('naver')">
            <span>N</span>
            Naver로 계속하기
          </button>
        </div>

      </div>

      <p class="login-foot">
        아직 계정이 없으신가요?
        <button type="button" @click="router.push('/signup')">회원가입</button>
      </p>
    </section>
  </main>
</template>
