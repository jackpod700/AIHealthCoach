<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import FormField from "../../components/auth/FormField.vue";
import SignupStepper from "../../components/auth/SignupStepper.vue";
import { signupSteps } from "../../constants/authOptions";
import { useAuthStore } from "../../stores/authStore";
import { useProfileStore } from "../../stores/profileStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const profileStore = useProfileStore();

const signupStep = ref(1);
const signupFieldErrors = reactive({
  nickname: "",
  email: "",
  password: "",
  heightCm: "",
  currentWeightKg: "",
  gender: "",
  age: "",
  form: "",
});

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
  if (signupStep.value < 2) {
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

function clearSignupFieldErrors() {
  Object.keys(signupFieldErrors).forEach((key) => {
    signupFieldErrors[key] = "";
  });
}

async function submitSignup() {
  if (signupStep.value < 2) {
    if (signupStep.value === 1 && !validateAccountStep()) {
      return;
    }

    clearSignupFieldErrors();
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

  await authStore.signup({
    email: signupForm.email.trim(),
    password: signupForm.password.trim(),
    nickname: signupForm.nickname.trim(),
  });

  if (authStore.isAuthenticated) {
    await profileStore.updateProfile({
      heightCm: Number(signupForm.heightCm),
      currentWeightKg: Number(signupForm.currentWeightKg),
      targetWeightKg: Number(signupForm.currentWeightKg),
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

  try {
    await profileStore.updateNickname(signupForm.nickname.trim());

    await profileStore.updateProfile({
      heightCm: toNumber(signupForm.heightCm),
      currentWeightKg: toNumber(signupForm.currentWeightKg),
      targetWeightKg: toNumber(signupForm.currentWeightKg),
      gender: signupForm.gender,
      age: toNumber(signupForm.age),
      goalType: signupForm.goalType,
    });

    router.push("/chat");
  } catch {
    signupFieldErrors.form =
      profileStore.profileError || "가입 정보를 저장하지 못했습니다.";
  }
}

function validateAccountStep() {
  clearSignupFieldErrors();

  const nickname = signupForm.nickname.trim();
  const email = signupForm.email.trim();
  const password = signupForm.password.trim();
  let isValid = true;

  if (isOAuthSignup.value) {
    if (!nickname) {
      signupFieldErrors.nickname = "닉네임을 입력해주세요.";
      isValid = false;
    }

    if (!email) {
      signupFieldErrors.email = "소셜 로그인 이메일을 확인하지 못했습니다.";
      isValid = false;
    }

    return isValid;
  }

  if (!nickname) {
    signupFieldErrors.nickname = "닉네임을 입력해주세요.";
    isValid = false;
  }

  if (!email) {
    signupFieldErrors.email = "이메일을 입력해주세요.";
    isValid = false;
  } else if (!isValidEmail(email)) {
    signupFieldErrors.email = "이메일 형식에 맞게 입력해주세요.";
    isValid = false;
  }

  if (!password) {
    signupFieldErrors.password = "비밀번호를 입력해주세요.";
    isValid = false;
  } else if (password.length < 8) {
    signupFieldErrors.password = "비밀번호는 8자 이상 입력해주세요.";
    isValid = false;
  }

  return isValid;
}

function validateBodyInfoStep() {
  clearSignupFieldErrors();

  const heightCm = toNumber(signupForm.heightCm);
  const currentWeightKg = toNumber(signupForm.currentWeightKg);
  const age = toNumber(signupForm.age);
  let isValid = true;

  if (heightCm === null || heightCm < 50 || heightCm > 300) {
    signupFieldErrors.heightCm = "키는 50cm 이상 300cm 이하로 입력해주세요.";
    isValid = false;
  }

  if (currentWeightKg === null || currentWeightKg < 1 || currentWeightKg > 999.99) {
    signupFieldErrors.currentWeightKg = "현재 체중은 1kg 이상 999.99kg 이하로 입력해주세요.";
    isValid = false;
  }

  if (age === null || age <= 0) {
    signupFieldErrors.age = "나이는 1 이상으로 입력해주세요.";
    isValid = false;
  }

  if (!["MALE", "FEMALE"].includes(signupForm.gender)) {
    signupFieldErrors.gender = "성별을 선택해주세요.";
    isValid = false;
  }

  return isValid;
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
</script>

<template>
  <main class="signup-screen">
    <header class="auth-topbar">
      <button type="button" class="auth-brand-link" @click="router.push('/')">
        <span class="brand-mark"><i class="pi pi-briefcase"></i></span>
        <span>
          <strong>BabStroy 시작하기</strong>
        </span>
      </button>
      <button type="button" class="auth-close-button" aria-label="랜딩으로 돌아가기" @click="router.push('/')">
        <i class="pi pi-times"></i>
      </button>
    </header>

    <section class="signup-shell">
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
            <div class="signup-field-group">
              <FormField
                v-model="signupForm.nickname"
                label="닉네임"
                icon="pi pi-user"
                placeholder="닉네임을 입력하세요"
                autocomplete="nickname"
              />
              <p class="field-error" :class="{ visible: signupFieldErrors.nickname }">
                {{ signupFieldErrors.nickname }}
              </p>
            </div>

            <template v-if="isOAuthSignup">
              <label class="signup-field-group">
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
                <p class="field-error" :class="{ visible: signupFieldErrors.email }">
                  {{ signupFieldErrors.email }}
                </p>
              </label>
            </template>

            <template v-else>
              <div class="signup-field-group">
                <FormField
                  v-model="signupForm.email"
                  label="이메일"
                  icon="pi pi-envelope"
                  type="email"
                  placeholder="이메일을 입력하세요"
                  autocomplete="email"
                />
                <p class="field-error" :class="{ visible: signupFieldErrors.email }">
                  {{ signupFieldErrors.email }}
                </p>
              </div>

              <div class="signup-field-group">
                <FormField
                  v-model="signupForm.password"
                  label="비밀번호"
                  icon="pi pi-lock"
                  type="password"
                  placeholder="8자 이상 입력하세요"
                  autocomplete="new-password"
                />
                <p class="field-error" :class="{ visible: signupFieldErrors.password }">
                  {{ signupFieldErrors.password }}
                </p>
              </div>
            </template>
          </div>
        </template>

        <template v-else-if="signupStep === 2">
          <p class="deco">Basic Info</p>
          <h1>기본 정보를 알려주세요</h1>
          <p class="signup-lead">키와 현재 체중을 기반으로 하루 섭취량과 코칭 기준을 계산해요.</p>

          <div class="signup-fields signup-basic-fields two-column">
            <label class="signup-field-group">
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
              <p class="field-error" :class="{ visible: signupFieldErrors.heightCm }">
                {{ signupFieldErrors.heightCm }}
              </p>
            </label>

            <label class="signup-field-group">
              <span>현재 체중</span>
              <div class="unit-field">
                <input
                  v-model="signupForm.currentWeightKg"
                  inputmode="decimal"
                  min="1"
                  max="999.99"
                  placeholder="65.2"
                  step="any"
                  type="number"
                />
                <em>kg</em>
              </div>
              <p class="field-error" :class="{ visible: signupFieldErrors.currentWeightKg }">
                {{ signupFieldErrors.currentWeightKg }}
              </p>
            </label>
            <label class="signup-field-group">
              <span>성별</span>
              <div class="segmented-field">
                <button type="button" :class="{ active: signupForm.gender === 'FEMALE' }" @click="signupForm.gender = 'FEMALE'">여성</button>
                <button type="button" :class="{ active: signupForm.gender === 'MALE' }" @click="signupForm.gender = 'MALE'">남성</button>
              </div>
              <p class="field-error" :class="{ visible: signupFieldErrors.gender }">
                {{ signupFieldErrors.gender }}
              </p>
            </label>
            <label class="signup-field-group">
              <span>나이</span>
              <div class="unit-field">
                <input v-model="signupForm.age" inputmode="numeric" min="1" placeholder="29" step="1" type="number" />
                <em>세</em>
              </div>
              <p class="field-error" :class="{ visible: signupFieldErrors.age }">
                {{ signupFieldErrors.age }}
              </p>
            </label>
          </div>

          <div class="signup-info-card">
            <i class="pi pi-sparkles"></i>
            <div>
              <strong>입력한 정보는 코칭 계산에만 사용돼요</strong>
              <span>목표와 세부 수치는 프로필에서 언제든 조정할 수 있어요.</span>
            </div>
          </div>
        </template>

        <p class="field-error form-field-error" :class="{ visible: signupFieldErrors.form || authStore.signupError || profileStore.profileError }" aria-live="polite">
          {{ signupFieldErrors.form || authStore.signupError || profileStore.profileError }}
        </p>

        <div class="signup-actions">
          <button class="secondary-action" type="button" @click="previousSignupStep">
            이전
          </button>

          <button
            class="primary-action"
            type="submit"
            :disabled="authStore.isSigningUp || profileStore.isSavingProfile"
          >
            {{ signupStep === 2 ? signupButtonLabel : "다음" }}
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
