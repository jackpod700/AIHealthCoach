<script setup>
import { computed, onMounted, reactive, watch } from "vue";
import { useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { goalOptions } from "../../constants/authOptions";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";

const authStore = useAuthStore();
const profileStore = useProfileStore();
const router = useRouter();

const profileForm = reactive({
  heightCm: "",
  currentWeightKg: "",
  targetWeightKg: "",
  gender: "FEMALE",
  age: "",
  goalType: "WEIGHT_LOSS",
});

const displayName = computed(() => {
  return authStore.user?.nickname || authStore.user?.email?.split("@")[0] || "사용자";
});

const avatarInitial = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase();
});

const goalLabel = computed(() => {
  return goalOptions.find((goal) => goal.value === profileForm.goalType)?.title || "목표 미설정";
});

const weightDiff = computed(() => {
  const current = Number(profileStore.profile?.currentWeightKg);
  const target = Number(profileStore.profile?.targetWeightKg);

  if (!Number.isFinite(current) || !Number.isFinite(target)) {
    return null;
  }

  return Math.abs(current - target).toFixed(1);
});

const profileUpdatedLabel = computed(() => {
  if (!profileStore.profile?.updatedAt) {
    return "최근 수정일 API 연결 필요";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(profileStore.profile.updatedAt));
});

onMounted(async () => {
  await profileStore.loadProfile();

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
});

watch(
  () => profileStore.profile,
  (profile) => {
    if (!profile) {
      return;
    }

    profileForm.heightCm = profile.heightCm ?? "";
    profileForm.currentWeightKg = profile.currentWeightKg ?? "";
    profileForm.targetWeightKg = profile.targetWeightKg ?? "";
    profileForm.gender = profile.gender || "FEMALE";
    profileForm.age = profile.age ?? "";
    profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
  },
  { immediate: true }
);

function resetForm() {
  const profile = profileStore.profile;

  if (!profile) {
    return;
  }

  profileForm.heightCm = profile.heightCm ?? "";
  profileForm.currentWeightKg = profile.currentWeightKg ?? "";
  profileForm.targetWeightKg = profile.targetWeightKg ?? "";
  profileForm.gender = profile.gender || "FEMALE";
  profileForm.age = profile.age ?? "";
  profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
}

async function saveProfile() {
  await profileStore.updateProfile({
    heightCm: Number(profileForm.heightCm),
    currentWeightKg: Number(profileForm.currentWeightKg),
    targetWeightKg: Number(profileForm.targetWeightKg),
    gender: profileForm.gender,
    age: Number(profileForm.age),
    goalType: profileForm.goalType,
  });

  if (!authStore.isAuthenticated) {
    router.replace("/login");
  }
}
</script>

<template>
  <main class="profile-home">
    <AppSidebar />

    <section class="profile-workspace">
      <header class="profile-header">
        <div>
          <p class="deco">Your Profile</p>
          <h1>프로필</h1>
        </div>
        <div class="streak-chip">
          <i></i>
          {{ goalLabel }} 목표 진행 중
        </div>
      </header>

      <div class="profile-body">
        <aside class="profile-summary-card">
          <div class="profile-avatar-large">{{ avatarInitial }}</div>
          <h2>{{ displayName }}</h2>
          <span class="goal-badge">{{ goalLabel }} 목표</span>

          <div class="profile-divider"></div>

          <div class="profile-stat-row">
            <div>
              <strong>{{ profileStore.profile?.heightCm ?? "-" }}</strong>
              <small>cm</small>
              <span>키</span>
            </div>
            <div>
              <strong>{{ profileStore.profile?.currentWeightKg ?? "-" }}</strong>
              <small>kg</small>
              <span>현재</span>
            </div>
            <div>
              <strong>{{ profileStore.profile?.targetWeightKg ?? "-" }}</strong>
              <small>kg</small>
              <span>목표</span>
            </div>
          </div>

          <div class="profile-progress-box">
            <div>
              <strong>
                {{ weightDiff ? `목표까지 ${weightDiff}kg` : "목표 체중 API 연결 필요" }}
              </strong>
              <span>시작 체중 API 연결 필요</span>
            </div>
            <div class="profile-progress-track">
              <i></i>
            </div>
            <p>{{ profileUpdatedLabel }}</p>
          </div>
        </aside>

        <section class="profile-edit-card">
          <div class="profile-edit-title">
            <h2>기본 정보 수정</h2>
            <p>정확한 정보일수록 코칭이 더 정밀해져요.</p>
          </div>

          <div v-if="profileStore.profileSuccess" class="profile-success">
            <i class="pi pi-check-circle"></i>
            {{ profileStore.profileSuccess }}
          </div>

          <div v-if="profileStore.profileError" class="profile-error">
            {{ profileStore.profileError }}
          </div>

          <div v-if="profileStore.isLoadingProfile" class="profile-loading">
            프로필 정보를 불러오는 중입니다...
          </div>

          <form v-else class="profile-form-grid" @submit.prevent="saveProfile">
            <label>
              <span>이름</span>
              <input :value="displayName" readonly />
            </label>
            <label>
              <span>키 (cm)</span>
              <input v-model="profileForm.heightCm" inputmode="decimal" />
            </label>
            <label>
              <span>현재 몸무게 (kg)</span>
              <input v-model="profileForm.currentWeightKg" class="focused-input" inputmode="decimal" />
            </label>
            <label>
              <span>목표 몸무게 (kg)</span>
              <input v-model="profileForm.targetWeightKg" inputmode="decimal" />
            </label>
            <label>
              <span>성별</span>
              <div class="segmented-field profile-segmented-field">
                <button type="button" :class="{ active: profileForm.gender === 'FEMALE' }" @click="profileForm.gender = 'FEMALE'">여성</button>
                <button type="button" :class="{ active: profileForm.gender === 'MALE' }" @click="profileForm.gender = 'MALE'">남성</button>
              </div>
            </label>
            <label>
              <span>나이</span>
              <input v-model="profileForm.age" inputmode="numeric" min="1" type="number" />
            </label>

            <fieldset>
              <legend>목표 유형</legend>
              <div class="profile-goal-buttons">
                <button
                  v-for="goal in goalOptions"
                  :key="goal.value"
                  type="button"
                  :class="{ active: profileForm.goalType === goal.value }"
                  @click="profileForm.goalType = goal.value"
                >
                  {{ goal.title }}
                </button>
              </div>
            </fieldset>

            <div class="profile-actions">
              <button class="profile-cancel" type="button" @click="resetForm">취소</button>
              <button class="profile-save" type="submit" :disabled="profileStore.isSavingProfile">
                {{ profileStore.isSavingProfile ? "저장 중..." : "변경 사항 저장" }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>
  </main>
</template>
