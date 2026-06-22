<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthBrand from "../../components/auth/AuthBrand.vue";
import FormField from "../../components/auth/FormField.vue";
import GoalOptionCard from "../../components/auth/GoalOptionCard.vue";
import SignupStepper from "../../components/auth/SignupStepper.vue";
import { goalOptions, signupSteps } from "../../constants/authOptions";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const profileStore = useProfileStore();

const signupStep = ref(1);
const signupValidationError = ref("");

const isOAuthSignup = computed(() => {
  return route.query.oauth === "true";
});

const signupForm = reactive({
  nickname: "",
  email: "",
  password: "",
  heightCm: "",
  currentWeightKg: "",
  gender: "FEMALE",
  age: "",
  targetWeightKg: "62",
  goalPeriodMonths: 3,
  goalType: "WEIGHT_LOSS",
});

const signupButtonLabel = computed(() => {
  if (isOAuthSignup.value) {
    return profileStore.isSavingProfile ? "저장 중..." : "시작하기";
  }

  return authStore.isSigningUp ? "가입 중..." : "시작하기";
});

onMounted(() => {
  if (!isOAuthSignup.value) {
    return;
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  signupForm.nickname = authStore.user?.nickname || "";
  signupForm.email = authStore.user?.email || "";
  signupForm.password = "";
});

function nextSignupStep() {
  if (signupStep.value < 3) {
    signupStep.value += 1;
  }
}

function previousSignupStep() {
  if (signupStep.value > 1) {
    signupStep.value -= 1;
    return;
  }

  router.push("/login");
}

function updateGoalPeriod(delta) {
  const nextValue = signupForm.goalPeriodMonths + delta;
  signupForm.goalPeriodMonths = Math.min(12, Math.max(1, nextValue));
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

async function submitSignup() {
  if (signupStep.value < 3) {
    if (signupStep.value === 1 && !validateAccountStep()) {
      return;
    }

    if (signupStep.value === 2 && !validateBodyInfoStep()) {
      return;
    }

    signupValidationError.value = "";
    nextSignupStep();
    return;
  }

  if (isOAuthSignup.value) {
    await submitOAuthSignup();
    return;
  }

  await submitNormalSignup();
}

async function submitNormalSignup() {
  if (authStore.isSigningUp) {
    return;
  }

  if (!validateAccountStep()) {
    signupStep.value = 1;
    return;
  }

  if (!validateBodyInfoStep()) {
    signupStep.value = 2;
    return;
  }

  if (!validateGoalStep()) {
    signupStep.value = 3;
    return;
  }

  await authStore.signup({
    email: signupForm.email.trim(),
    password: signupForm.password.trim(),
    nickname: signupForm.nickname.trim(),
  });

  if (authStore.isAuthenticated) {
    await profileStore.updateProfile({
      heightCm: Number(signupForm.heightCm),
      currentWeightKg: Number(signupForm.currentWeightKg),
      targetWeightKg: Number(signupForm.targetWeightKg),
      gender: signupForm.gender,
      age: Number(signupForm.age),
      goalType: signupForm.goalType,
    });

    router.push("/chat");
  }
}

async function submitOAuthSignup() {
  if (profileStore.isSavingProfile) {
    return;
  }

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (!validateAccountStep()) {
    signupStep.value = 1;
    return;
  }

  if (!validateBodyInfoStep()) {
    signupStep.value = 2;
    return;
  }

  if (!validateGoalStep()) {
    signupStep.value = 3;
    return;
  }

  try {
    await profileStore.updateNickname(signupForm.nickname.trim());

    await profileStore.updateProfile({
      heightCm: toNumber(signupForm.heightCm),
      currentWeightKg: toNumber(signupForm.currentWeightKg),
      targetWeightKg: toNumber(signupForm.targetWeightKg),
      goalType: signupForm.goalType,
    });

    router.push("/chat");
  } catch {
    signupValidationError.value =
      profileStore.profileError || "가입 정보를 저장하지 못했습니다.";
  }
}

function validateAccountStep() {
  const nickname = signupForm.nickname.trim();
  const email = signupForm.email.trim();
  const password = signupForm.password.trim();

  if (isOAuthSignup.value) {
    if (!nickname) {
      signupValidationError.value = "닉네임을 입력해주세요.";
      return false;
    }

    if (!email) {
      signupValidationError.value = "소셜 로그인 이메일을 확인하지 못했습니다.";
      return false;
    }

    signupValidationError.value = "";
    return true;
  }

  if (!nickname || !email || !password) {
    signupValidationError.value = "닉네임, 이메일, 비밀번호를 모두 입력해주세요.";
    return false;
  }

  if (!isValidEmail(email)) {
    signupValidationError.value = "이메일 형식에 맞게 입력해주세요.";
    return false;
  }

  if (password.length < 8) {
    signupValidationError.value = "비밀번호는 8자 이상 입력해주세요.";
    return false;
  }

  signupValidationError.value = "";
  return true;
}

function validateBodyInfoStep() {
  const heightCm = toNumber(signupForm.heightCm);
  const currentWeightKg = toNumber(signupForm.currentWeightKg);
  const age = toNumber(signupForm.age);

  if (heightCm === null || heightCm < 50 || heightCm > 300) {
    signupValidationError.value = "키는 50cm 이상 300cm 이하로 입력해주세요.";
    return false;
  }

  if (currentWeightKg === null || currentWeightKg < 1 || currentWeightKg > 999.99) {
    signupValidationError.value = "현재 체중은 1kg 이상 999.99kg 이하로 입력해주세요.";
    return false;
  }

  if (age === null || age <= 0) {
    signupValidationError.value = "나이는 1 이상으로 입력해주세요.";
    return false;
  }

  if (!["MALE", "FEMALE"].includes(signupForm.gender)) {
    signupValidationError.value = "성별을 선택해주세요.";
    return false;
  }

  signupValidationError.value = "";
  return true;
}

function validateGoalStep() {
  const targetWeightKg = toNumber(signupForm.targetWeightKg);

  if (targetWeightKg === null || targetWeightKg < 1 || targetWeightKg > 999.99) {
    signupValidationError.value = "목표 체중은 1kg 이상 999.99kg 이하로 입력해주세요.";
    return false;
  }

  if (!signupForm.goalType) {
    signupValidationError.value = "목표 유형을 선택해주세요.";
    return false;
  }

  signupValidationError.value = "";
  return true;
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
</script>

<template>
  <main class="signup-screen">
    <section class="signup-shell">
      <AuthBrand compact />
      <SignupStepper :steps="signupSteps" :active-step="signupStep" />

      <form class="signup-card" novalidate @submit.prevent="submitSignup">
        <template v-if="signupStep === 1">
          <p class="deco">Create Account</p>

          <h1>
            {{ isOAuthSignup ? "가입 정보를 확인해주세요" : "계정을 만들어볼까요?" }}
          </h1>

          <p class="signup-lead">
            {{
              isOAuthSignup
                ? "소셜 로그인으로 확인된 이메일을 기반으로 기본 정보를 설정해요."
                : "코칭 기록을 안전하게 저장할 수 있도록 기본 계정을 먼저 만들어요."
            }}
          </p>

          <div class="signup-fields">
            <FormField
              v-model="signupForm.nickname"
              label="닉네임"
              icon="pi pi-user"
              placeholder="닉네임을 입력하세요"
              autocomplete="nickname"
            />

            <template v-if="isOAuthSignup">
              <label>
                <span>이메일</span>
                <div class="field-shell">
                  <i class="pi pi-envelope"></i>
                  <input
                    :value="signupForm.email"
                    type="email"
                    readonly
                    autocomplete="email"
                    placeholder="소셜 로그인 이메일"
                  />
                </div>
              </label>
            </template>

            <template v-else>
              <FormField
                v-model="signupForm.email"
                label="이메일"
                icon="pi pi-envelope"
                type="email"
                placeholder="이메일을 입력하세요"
                autocomplete="email"
              />

              <FormField
                v-model="signupForm.password"
                label="비밀번호"
                icon="pi pi-lock"
                type="password"
                placeholder="8자 이상 입력하세요"
                autocomplete="new-password"
              />
            </template>
          </div>
        </template>

        <template v-else-if="signupStep === 2">
          <p class="deco">Basic Info</p>
          <h1>기본 정보를 알려주세요</h1>
          <p class="signup-lead">키와 현재 체중을 기반으로 하루 섭취량과 코칭 기준을 계산해요.</p>

          <div class="signup-fields two-column">
            <label>
              <span>키</span>
              <div class="unit-field">
                <input
                  v-model="signupForm.heightCm"
                  inputmode="decimal"
                  min="50"
                  max="300"
                  placeholder="168"
                  step="0.1"
                  type="number"
                />
                <em>cm</em>
              </div>
            </label>

            <label>
              <span>현재 체중</span>
              <div class="unit-field">
                <input
                  v-model="signupForm.currentWeightKg"
                  inputmode="decimal"
                  min="1"
                  max="999.99"
                  placeholder="65.2"
                  step="0.1"
                  type="number"
                />
                <em>kg</em>
              </div>
            </label>
            <label>
              <span>성별</span>
              <div class="segmented-field">
                <button type="button" :class="{ active: signupForm.gender === 'FEMALE' }" @click="signupForm.gender = 'FEMALE'">여성</button>
                <button type="button" :class="{ active: signupForm.gender === 'MALE' }" @click="signupForm.gender = 'MALE'">남성</button>
              </div>
            </label>
            <label>
              <span>나이</span>
              <div class="unit-field">
                <input v-model="signupForm.age" inputmode="numeric" min="1" placeholder="29" step="1" type="number" />
                <em>세</em>
              </div>
            </label>
          </div>

          <div class="signup-info-card">
            <i class="pi pi-sparkles"></i>
            <div>
              <strong>입력한 정보는 코칭 계산에만 사용돼요</strong>
              <span>목표 체중과 기간은 다음 단계에서 설정할 수 있어요.</span>
            </div>
          </div>
        </template>

        <template v-else>
          <p class="deco">Almost There</p>
          <h1>어떤 목표를 향해 갈까요?</h1>
          <p class="signup-lead">목표에 맞춰 코치가 칼로리와 코칭 톤을 조절해요. 나중에 바꿀 수 있어요.</p>

          <div class="goal-grid">
            <GoalOptionCard
              v-for="goal in goalOptions"
              :key="goal.value"
              :goal="goal"
              :selected="signupForm.goalType === goal.value"
              @select="signupForm.goalType = $event"
            />
          </div>

          <div class="signup-fields two-column">
            <label>
              <span>목표 몸무게</span>
              <div class="unit-field focused">
                <input
                  v-model="signupForm.targetWeightKg"
                  inputmode="decimal"
                  min="1"
                  max="999.99"
                  step="0.1"
                  type="number"
                />
                <em>kg</em>
              </div>
            </label>

            <label>
              <span>목표 기간</span>
              <div class="period-stepper">
                <button
                  type="button"
                  aria-label="목표 기간 줄이기"
                  :disabled="signupForm.goalPeriodMonths <= 1"
                  @click="updateGoalPeriod(-1)"
                >
                  <i class="pi pi-minus"></i>
                </button>

                <strong>{{ signupForm.goalPeriodMonths }}개월</strong>

                <button
                  type="button"
                  aria-label="목표 기간 늘리기"
                  :disabled="signupForm.goalPeriodMonths >= 12"
                  @click="updateGoalPeriod(1)"
                >
                  <i class="pi pi-plus"></i>
                </button>
              </div>
            </label>
          </div>
        </template>

        <div
          v-if="signupValidationError || authStore.signupError || profileStore.profileError"
          class="login-error signup-error"
        >
          {{ signupValidationError || authStore.signupError || profileStore.profileError }}
        </div>

        <div class="signup-actions">
          <button class="secondary-action" type="button" @click="previousSignupStep">
            이전
          </button>

          <button
            class="primary-action"
            type="submit"
            :disabled="authStore.isSigningUp || profileStore.isSavingProfile"
          >
            {{ signupStep === 3 ? signupButtonLabel : "다음" }}
          </button>
        </div>
      </form>

      <p v-if="!isOAuthSignup" class="signup-foot">
        이미 계정이 있으신가요?
        <button type="button" @click="router.push('/login')">로그인</button>
      </p>
    </section>
  </main>
</template>
