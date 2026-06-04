<template>
  <section class="login-visual">
    <AuthBrand />

    <div class="login-hero">
      <p class="deco">Welcome Back</p>
      <h2>
        대화하듯<br />
        관리하는 건강
      </h2>
      <p>
        복잡한 입력 폼은 그만. 오늘 먹은 것과 운동을 말하듯 기록하면,
        AI 코치가 당신에게 꼭 맞는 가이드를 건넵니다.
      </p>
    </div>

    <div class="login-quote">
      <div class="quote-avatar">민</div>
      <div>
        <strong>“출근길에 한 줄 적었을 뿐인데, 3주 만에 2kg 감량했어요.”</strong>
        <span>박민서 · 직장인</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import AuthBrand from "./AuthBrand.vue";
</script>
