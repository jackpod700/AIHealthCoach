<script setup>
import { useRouter } from "vue-router";
import AuthBrand from "../../components/auth/AuthBrand.vue";

const router = useRouter();
</script>

<template>
  <main class="error-page not-found-page">
    <header class="error-brand">
      <AuthBrand />
    </header>

    <section class="error-center">
      <div class="error-code">
        <span>4</span>
        <i></i>
        <span>4</span>
      </div>

      <p class="deco">Page Not Found</p>
      <h1>길을 잃으셨네요</h1>
      <p class="error-description">
        찾으시는 페이지가 없거나 다른 곳으로 옮겨졌어요. 주소를 다시 확인하거나 홈으로 돌아가 주세요.
      </p>

      <div class="error-actions">
        <button class="error-primary" type="button" @click="router.push('/')">
          <i class="pi pi-home"></i>
          홈으로 돌아가기
        </button>
        <button class="error-secondary" type="button" @click="router.push('/chat')">
          <i class="pi pi-comment"></i>
          코치에게 물어보기
        </button>
      </div>

      <p class="error-links">
        도움이 필요하신가요?
        <a href="#">고객센터</a>
        <span>·</span>
        <a href="#">상태 페이지</a>
      </p>
    </section>
  </main>
</template>
