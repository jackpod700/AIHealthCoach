<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";

const router = useRouter();
const authStore = useAuthStore();
const profileStore = useProfileStore();

const message = ref("소셜 로그인 정보를 확인하고 있어요...");

function isProfileCompleted(profile) {
  return (
    profile?.heightCm != null &&
    profile?.currentWeightKg != null &&
    profile?.targetWeightKg != null &&
    profile?.goalType != null &&
    profile?.goalType !== ""
  );
}

onMounted(async () => {
  try {
    await authStore.completeOAuthLogin();

    if (!authStore.isAuthenticated) {
      throw new Error("로그인 상태를 확인하지 못했습니다.");
    }

    message.value = "가입 정보를 확인하고 있어요...";

    profileStore.clearProfile();
    await profileStore.loadProfile();

    if (isProfileCompleted(profileStore.profile)) {
      router.replace("/chat");
      return;
    }

    router.replace({
      path: "/signup",
      query: {
        oauth: "true",
      },
    });
  } catch (error) {
    message.value = error.message || "소셜 로그인 처리 중 오류가 발생했습니다.";

    setTimeout(() => {
      router.replace("/login");
    }, 1200);
  }
});
</script>

<template>
  <main class="login-screen">
    <section class="login-panel oauth-success-panel" aria-label="소셜 로그인 처리">
      <div class="login-card">
        <p class="deco">OAuth Login</p>
        <h2>소셜 로그인 처리 중</h2>
        <p class="login-lead">{{ message }}</p>
      </div>
    </section>
  </main>
</template>