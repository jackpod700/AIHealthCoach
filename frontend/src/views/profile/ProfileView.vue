<script setup>
import { computed, onMounted, reactive, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppSidebar from "../../components/app/AppSidebar.vue";
import { goalOptions } from "../../constants/authOptions";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";


const authStore = useAuthStore();
const profileStore = useProfileStore();
const router = useRouter();
const route = useRoute();

const isSetupMode = computed(() => {
  return route.path === "/profile/setup";
});

const profileForm = reactive({
  heightCm: "",
  currentWeightKg: "",
  targetWeightKg: "",
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
    return "최근 수정일 없음";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(profileStore.profile.updatedAt));
});

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  await profileStore.loadProfile();
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
    profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
  },
  { immediate: true }
);

function resetForm() {
  const profile = profileStore.profile;

  if (!profile) {
    profileForm.heightCm = "";
    profileForm.currentWeightKg = "";
    profileForm.targetWeightKg = "";
    profileForm.goalType = "WEIGHT_LOSS";
    return;
  }

  profileForm.heightCm = profile.heightCm ?? "";
  profileForm.currentWeightKg = profile.currentWeightKg ?? "";
  profileForm.targetWeightKg = profile.targetWeightKg ?? "";
  profileForm.goalType = profile.goalType || "WEIGHT_LOSS";
}

function toNumber(value) {
  if (value === null || value === undefined || String(value).trim() === "") {
    return null;
  }

  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return null;
  }

  return numberValue;
}

function validateProfileForm() {
  const heightCm = toNumber(profileForm.heightCm);
  const currentWeightKg = toNumber(profileForm.currentWeightKg);
  const targetWeightKg = toNumber(profileForm.targetWeightKg);

  if (heightCm === null || heightCm < 50 || heightCm > 300) {
    return "키는 50cm 이상 300cm 이하로 입력해주세요.";
  }

  if (currentWeightKg === null || currentWeightKg < 1 || currentWeightKg > 999.99) {
    return "현재 몸무게는 1kg 이상 999.99kg 이하로 입력해주세요.";
  }

  if (targetWeightKg === null || targetWeightKg < 1 || targetWeightKg > 999.99) {
    return "목표 몸무게는 1kg 이상 999.99kg 이하로 입력해주세요.";
  }

  if (!profileForm.goalType) {
    return "목표 유형을 선택해주세요.";
  }

  return null;
}

async function saveProfile() {
  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  const validationMessage = validateProfileForm();

  if (validationMessage) {
    profileStore.profileError = validationMessage;
    return;
  }

  await profileStore.updateProfile({
    heightCm: toNumber(profileForm.heightCm),
    currentWeightKg: toNumber(profileForm.currentWeightKg),
    targetWeightKg: toNumber(profileForm.targetWeightKg),
    goalType: profileForm.goalType,
  });

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (!profileStore.profileError && isSetupMode.value) {
    router.replace("/chat");
  }
}
</script>

<template>
  <main class="profile-home">
    <AppSidebar />

    <section class="profile-workspace">
      <header class="profile-header">
        <div>
          <p class="deco">
            {{ isSetupMode ? "Profile Setup" : "Your Profile" }}
          </p>
          <h1>
            {{ isSetupMode ? "기본 정보 설정" : "프로필" }}
          </h1>
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

          <span class="goal-badge">
            {{ goalLabel }} 목표
          </span>

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
                {{ weightDiff ? `목표까지 ${weightDiff}kg` : "목표 체중을 설정해주세요" }}
              </strong>
              <span>프로필 정보를 입력하면 맞춤 코칭이 시작돼요</span>
            </div>

            <div class="profile-progress-track">
              <i></i>
            </div>

            <p>{{ profileUpdatedLabel }}</p>
          </div>
        </aside>

        <section class="profile-edit-card">
          <div class="profile-edit-title">
            <h2>
              {{ isSetupMode ? "기본 정보 입력" : "기본 정보 수정" }}
            </h2>
            <p>
              {{
                isSetupMode
                  ? "맞춤형 건강 코칭을 위해 기본 정보와 목표를 입력해주세요."
                  : "정확한 정보일수록 코칭이 더 정밀해져요."
              }}
            </p>
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
              <input
                v-model="profileForm.heightCm"
                type="number"
                min="50"
                max="300"
                step="0.01"
                inputmode="decimal"
                placeholder="예: 170"
              />
            </label>

            <label>
              <span>현재 몸무게 (kg)</span>
              <input
                v-model="profileForm.currentWeightKg"
                type="number"
                min="1"
                max="999.99"
                step="0.01"
                class="focused-input"
                inputmode="decimal"
                placeholder="예: 70"
              />
            </label>

            <label>
              <span>목표 몸무게 (kg)</span>
              <input
                v-model="profileForm.targetWeightKg"
                type="number"
                min="1"
                max="999.99"
                step="0.01"
                inputmode="decimal"
                placeholder="예: 65"
              />
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
              <button
                v-if="!isSetupMode"
                class="profile-cancel"
                type="button"
                @click="resetForm"
              >
                취소
              </button>

              <button
                class="profile-save"
                type="submit"
                :disabled="profileStore.isSavingProfile"
              >
                {{
                  profileStore.isSavingProfile
                    ? "저장 중..."
                    : isSetupMode
                      ? "시작하기"
                      : "변경 사항 저장"
                }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>
  </main>
</template>