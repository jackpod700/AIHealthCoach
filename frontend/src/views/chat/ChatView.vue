<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { marked } from "marked";
import { useRouter } from "vue-router";
import DailyGoalSetupCard from "../../components/chat/DailyGoalSetupCard.vue";
import ExerciseProposalCard from "../../components/chat/ExerciseProposalCard.vue";
import MealProposalCard from "../../components/chat/MealProposalCard.vue";
import WeightProposalCard from "../../components/chat/WeightProposalCard.vue";
import { useAuthStore } from "../../stores/authStore";
import { useChatStore } from "../../stores/chatStore";
import { useDailyGoalStore } from "../../stores/dailyGoalStore";
import { useExerciseStore } from "../../stores/exerciseStore";
import { useMealStore } from "../../stores/mealStore";
import { useProfileStore } from "../../stores/profileStore";
import { useWeightRecordStore } from "../../stores/weightRecordStore";

const authStore = useAuthStore();
const chatStore = useChatStore();
const dailyGoalStore = useDailyGoalStore();
const exerciseStore = useExerciseStore();
const mealStore = useMealStore();
const profileStore = useProfileStore();
const weightRecordStore = useWeightRecordStore();
const router = useRouter();
const message = ref("");
const threadRef = ref(null);
const fileInputRef = ref(null);
const attachedImages = ref([]);
const imageAttachmentError = ref("");
const isDraggingImage = ref(false);
const isInitialScrollReady = ref(false);
const GMS_IMAGE_TARGET_BYTES = 7 * 1024;
const GMS_IMAGE_MAX_DIMENSION = 512;

const todayDateKey = computed(() => toDateKey(new Date()));

const displayMessages = computed(() => {
  return chatStore.orderedMessages;
});

const hasMessages = computed(() => {
  return displayMessages.value.length > 0;
});

watch(
  () =>
    displayMessages.value.map((chatMessage) => chatMessage.content).join("\n"),
  () => {
    if (chatStore.isSending) {
      void scrollToBottom();
    }
  },
);

marked.setOptions({
  breaks: true,
  gfm: true,
});

onMounted(async () => {
  isInitialScrollReady.value = false;
  window.addEventListener("paste", handlePaste);

  await Promise.all([
    chatStore.loadMessages(),
    mealStore.loadDailyMeal(todayDateKey.value),
    profileStore.loadProfile(),
    dailyGoalStore.loadProgress(todayDateKey.value),
  ]);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (dailyGoalStore.needsGoalSetup) {
    await dailyGoalStore.loadRecommendations(
      profileStore.profile?.goalType || "WEIGHT_LOSS",
    );
  }

  await scrollToBottom();
  isInitialScrollReady.value = true;
});

onBeforeUnmount(() => {
  window.removeEventListener("paste", handlePaste);
  revokeAttachedImageUrls();
});

async function sendMessage() {
  const content = message.value.trim();
  const images = attachedImages.value.map((image) => image.file);

  if ((!content && !images.length) || chatStore.isSending) {
    return;
  }

  message.value = "";
  clearAttachedImages();
  if (images.length) {
    await chatStore.sendImageMessage(content, images);
  } else {
    await chatStore.sendMessage(content);
  }

  const hasProposal = Boolean(
    chatStore.mealProposal ||
    chatStore.exerciseProposal ||
    chatStore.weightProposal,
  );

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  await mealStore.loadDailyMeal(todayDateKey.value);
  await dailyGoalStore.loadProgress(todayDateKey.value);

  if (!hasProposal) {
    await scrollToBottom();
  }
}

function openImagePicker() {
  fileInputRef.value?.click();
}

async function handleImageInput(event) {
  await addImageFiles(Array.from(event.target.files || []));
  event.target.value = "";
}

function handlePaste(event) {
  const files = Array.from(event.clipboardData?.files || []).filter((file) =>
    file.type.startsWith("image/"),
  );

  if (files.length) {
    void addImageFiles(files);
  }
}

function handleDragEnter(event) {
  if (hasImageFiles(event.dataTransfer)) {
    isDraggingImage.value = true;
  }
}

function handleDragOver(event) {
  if (hasImageFiles(event.dataTransfer)) {
    isDraggingImage.value = true;
  }
}

function handleDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    isDraggingImage.value = false;
  }
}

function handleDrop(event) {
  isDraggingImage.value = false;
  const files = Array.from(event.dataTransfer?.files || []).filter((file) =>
    file.type.startsWith("image/"),
  );

  if (files.length) {
    void addImageFiles(files);
  }
}

function hasImageFiles(dataTransfer) {
  return Array.from(dataTransfer?.items || []).some(
    (item) => item.kind === "file" && item.type.startsWith("image/"),
  );
}

async function addImageFiles(files = []) {
  imageAttachmentError.value = "";

  for (const file of files) {
    const error = validateImageFile(file);
    if (error) {
      imageAttachmentError.value = error;
      continue;
    }

    try {
      const compressedFile = await compressImageForGms(file);

      attachedImages.value.push({
        id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
        file: compressedFile,
        originalFile: file,
        previewUrl: URL.createObjectURL(file),
      });
    } catch (compressionError) {
      imageAttachmentError.value = compressionError.message;
    }
  }

  const totalSize = attachedImages.value.reduce(
    (sum, image) => sum + image.file.size,
    0,
  );
  if (totalSize > 50 * 1024) {
    const removedImage = attachedImages.value.pop();
    if (removedImage) {
      URL.revokeObjectURL(removedImage.previewUrl);
    }
    imageAttachmentError.value =
      "분석용 이미지는 한 번에 최대 50KB까지만 보낼 수 있어요.";
  }
}

function validateImageFile(file) {
  const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp"]);

  if (!allowedTypes.has(file.type)) {
    return "JPEG, PNG, WebP 이미지만 첨부할 수 있어요.";
  }

  if (file.size > 10 * 1024 * 1024) {
    return "이미지 1장은 최대 10MB까지만 첨부할 수 있어요.";
  }

  return "";
}

async function compressImageForGms(file) {
  if (file.size <= GMS_IMAGE_TARGET_BYTES) {
    return file;
  }

  const image = await loadImage(file);
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");

  if (!context) {
    throw new Error(
      "이미지 압축을 준비하지 못했어요. 다른 사진으로 다시 시도해주세요.",
    );
  }

  const maxDimensions = [GMS_IMAGE_MAX_DIMENSION, 384, 256, 192, 160];
  const qualities = [0.72, 0.6, 0.5, 0.42, 0.34, 0.28, 0.22];

  for (const maxDimension of maxDimensions) {
    const { width, height } = fitImageSize(
      image.width,
      image.height,
      maxDimension,
    );
    canvas.width = width;
    canvas.height = height;
    context.clearRect(0, 0, width, height);
    context.drawImage(image, 0, 0, width, height);

    for (const quality of qualities) {
      const blob = await canvasToBlob(canvas, quality);
      if (blob.size <= GMS_IMAGE_TARGET_BYTES) {
        return new File([blob], toCompressedFileName(file.name), {
          type: "image/jpeg",
          lastModified: Date.now(),
        });
      }
    }
  }

  throw new Error(
    "이미지가 너무 커서 실패했어요. 더 단순하거나 작은 사진으로 다시 시도해주세요.",
  );
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    const objectUrl = URL.createObjectURL(file);

    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(
        new Error("이미지를 읽지 못했어요. 다른 사진으로 다시 시도해주세요."),
      );
    };
    image.src = objectUrl;
  });
}

function fitImageSize(width, height, maxDimension) {
  const ratio = Math.min(maxDimension / width, maxDimension / height, 1);

  return {
    width: Math.max(1, Math.round(width * ratio)),
    height: Math.max(1, Math.round(height * ratio)),
  };
}

function canvasToBlob(canvas, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob);
          return;
        }

        reject(
          new Error(
            "이미지를 압축하지 못했어요. 다른 사진으로 다시 시도해주세요.",
          ),
        );
      },
      "image/jpeg",
      quality,
    );
  });
}

function toCompressedFileName(name = "meal-image") {
  return `${name.replace(/\.[^.]+$/, "")}-gms.jpg`;
}

function formatBytes(bytes) {
  if (bytes < 1024) {
    return `${bytes}B`;
  }

  return `${(bytes / 1024).toFixed(1)}KB`;
}

function removeAttachedImage(imageId) {
  const targetImage = attachedImages.value.find(
    (image) => image.id === imageId,
  );
  if (targetImage) {
    URL.revokeObjectURL(targetImage.previewUrl);
  }

  attachedImages.value = attachedImages.value.filter(
    (image) => image.id !== imageId,
  );
  imageAttachmentError.value = "";
}

function clearAttachedImages() {
  revokeAttachedImageUrls();
  attachedImages.value = [];
  imageAttachmentError.value = "";
}

function revokeAttachedImageUrls() {
  attachedImages.value.forEach((image) =>
    URL.revokeObjectURL(image.previewUrl),
  );
}

async function confirmMealProposal(payload) {
  const response = await chatStore.confirmMealProposal(payload);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (response?.dailyMeal) {
    mealStore.dailyMeal = response.dailyMeal;
  } else {
    await mealStore.loadDailyMeal(todayDateKey.value);
  }

  await dailyGoalStore.loadProgress(todayDateKey.value);
  await scrollToBottom();
}

async function confirmExerciseProposal(payload) {
  if (!chatStore.startConfirmingExercise()) {
    return;
  }

  const saved = await exerciseStore.saveRecord(payload);

  if (!authStore.isAuthenticated) {
    chatStore.finishConfirmingExercise();
    router.replace("/login");
    return;
  }

  if (saved) {
    chatStore.completeExerciseProposal();
    await dailyGoalStore.loadProgress(todayDateKey.value);
  } else {
    chatStore.failExerciseProposal(
      exerciseStore.saveRecordError || "운동 기록 저장에 실패했습니다.",
    );
  }

  chatStore.finishConfirmingExercise();
  await scrollToBottom();
}

async function confirmWeightProposal(payload) {
  if (!chatStore.startConfirmingWeight()) {
    return;
  }

  const saved = await weightRecordStore.saveRecord(payload);

  if (!authStore.isAuthenticated) {
    chatStore.finishConfirmingWeight();
    router.replace("/login");
    return;
  }

  if (saved) {
    chatStore.completeWeightProposal();
    await profileStore.loadProfile();
  } else {
    chatStore.failWeightProposal(
      weightRecordStore.saveError || "몸무게 기록 저장에 실패했습니다.",
    );
  }

  chatStore.finishConfirmingWeight();
  await scrollToBottom();
}

async function recommendDailyGoal(goalType) {
  if (dailyGoalStore.recommendations) {
    dailyGoalStore.selectRecommendation(goalType);
    return;
  }

  await dailyGoalStore.loadRecommendations(goalType);
}

async function saveDailyGoal(goal) {
  const savedGoal = await dailyGoalStore.saveGoal(goal);

  if (!authStore.isAuthenticated) {
    router.replace("/login");
    return;
  }

  if (savedGoal) {
    await dailyGoalStore.loadProgress(todayDateKey.value);
    await profileStore.loadProfile();
  }

  await scrollToBottom();
}

async function scrollToBottom() {
  await nextTick();

  if (threadRef.value) {
    threadRef.value.scrollTop = threadRef.value.scrollHeight;
  }
}

function isUserMessage(chatMessage) {
  return chatMessage.role === "USER";
}

function pendingAssistantText(chatMessage) {
  return chatMessage.pending ? "AI 코치가 답변을 준비하고 있어요..." : "";
}

function formatMessageTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function renderMarkdown(content = "") {
  return sanitizeHtml(marked.parse(content));
}

function sanitizeHtml(html = "") {
  const template = document.createElement("template");
  template.innerHTML = html;

  const allowedTags = new Set([
    "P",
    "STRONG",
    "EM",
    "UL",
    "OL",
    "LI",
    "TABLE",
    "THEAD",
    "TBODY",
    "TR",
    "TH",
    "TD",
    "H1",
    "H2",
    "H3",
    "H4",
    "BR",
    "CODE",
    "PRE",
  ]);

  template.content.querySelectorAll("*").forEach((element) => {
    if (!allowedTags.has(element.tagName)) {
      element.replaceWith(document.createTextNode(element.textContent || ""));
      return;
    }

    [...element.attributes].forEach((attribute) => {
      element.removeAttribute(attribute.name);
    });
  });

  return template.innerHTML;
}
</script>

<template>
  <div class="chat-body">
    <section
      class="chat-thread"
      :class="{ 'dragging-image': isDraggingImage }"
      @dragenter.prevent="handleDragEnter"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <header class="chat-header subtle">
        <div>
          <p class="deco">TODAY'S COACHING</p>
          <h1>오늘의 코칭</h1>
        </div>
        <div class="chat-status-pill">
          <i></i>
          <span>코칭 진행 중</span>
        </div>
      </header>

      <div
        class="chat-scroll"
        :class="{ initializing: !isInitialScrollReady }"
        ref="threadRef"
      >
        <div class="chat-message-stack">
          <div v-if="chatStore.isLoading" class="chat-state-card">
            채팅 기록을 불러오는 중입니다...
          </div>

          <div v-else-if="chatStore.error" class="chat-state-card error">
            {{ chatStore.error }}
          </div>

          <div
            v-else-if="!hasMessages && !dailyGoalStore.needsGoalSetup"
            class="chat-state-card"
          >
            아직 대화 기록이 없어요. 식단이나 운동을 편하게 입력해보세요.
          </div>

          <div v-if="dailyGoalStore.needsGoalSetup" class="message-row coach">
            <div class="coach-icon">
              <i class="pi pi-compass"></i>
            </div>
            <DailyGoalSetupCard
              :initial-goal-type="profileStore.profile?.goalType || 'WEIGHT_LOSS'"
              :recommendation="dailyGoalStore.recommendation"
              :is-loading-recommendation="dailyGoalStore.isLoadingRecommendation"
              :is-saving="dailyGoalStore.isSavingGoal"
              :recommendation-error="dailyGoalStore.recommendationError"
              :save-error="dailyGoalStore.saveGoalError"
              @recommend="recommendDailyGoal"
              @save="saveDailyGoal"
            />
          </div>

          <template
            v-for="chatMessage in displayMessages"
            :key="chatMessage.id || chatMessage.clientId"
          >
            <div v-if="isUserMessage(chatMessage)" class="message-row user">
              <div class="user-message-line">
                <time class="message-time">
                  {{ formatMessageTime(chatMessage.createdAt) }}
                </time>
                <div class="message-bubble user-bubble">
                  {{ chatMessage.content }}
                </div>
              </div>
            </div>

            <div v-else class="message-row coach">
              <div class="coach-icon">
                <i class="pi pi-briefcase"></i>
              </div>
              <div class="assistant-message-stack">
                <div class="assistant-name">AI 코치</div>
                <div class="assistant-message-line">
                  <article
                    class="assistant-card"
                    :class="{
                      pending: chatMessage.pending,
                      failed: chatMessage.failed,
                    }"
                  >
                    <div
                      class="markdown-content"
                      v-html="
                        renderMarkdown(
                          chatMessage.content ||
                            pendingAssistantText(chatMessage),
                        )
                      "
                    ></div>
                  </article>
                  <time class="message-time">
                    {{ formatMessageTime(chatMessage.createdAt) }}
                  </time>
                </div>
              </div>
            </div>
          </template>

          <div v-if="chatStore.mealProposal" class="message-row coach">
            <div class="coach-icon">
              <i class="pi pi-briefcase"></i>
            </div>
            <MealProposalCard
              :proposal="chatStore.mealProposal"
              :is-confirming="chatStore.isConfirmingMeal"
              :error="chatStore.mealProposalError"
              @confirm="confirmMealProposal"
              @dismiss="chatStore.dismissMealProposal"
            />
          </div>

          <div v-if="chatStore.exerciseProposal" class="message-row coach">
            <div class="coach-icon">
              <i class="pi pi-bolt"></i>
            </div>
            <ExerciseProposalCard
              :proposal="chatStore.exerciseProposal"
              :is-confirming="chatStore.isConfirmingExercise"
              :error="chatStore.exerciseProposalError"
              @confirm="confirmExerciseProposal"
              @dismiss="chatStore.dismissExerciseProposal"
            />
          </div>

          <div v-if="chatStore.weightProposal" class="message-row coach">
            <div class="coach-icon">
              <i class="pi pi-chart-line"></i>
            </div>
            <WeightProposalCard
              :proposal="chatStore.weightProposal"
              :is-confirming="chatStore.isConfirmingWeight"
              :error="chatStore.weightProposalError"
              @confirm="confirmWeightProposal"
              @dismiss="chatStore.dismissWeightProposal"
            />
          </div>
        </div>
      </div>

      <div
        v-if="attachedImages.length || imageAttachmentError"
        class="image-attachment-tray"
      >
        <div v-if="attachedImages.length" class="image-attachment-list">
          <figure v-for="image in attachedImages" :key="image.id">
            <img :src="image.previewUrl" :alt="image.file.name" />
            <figcaption>
              {{ image.originalFile.name }} ·
              {{ formatBytes(image.file.size) }}
            </figcaption>
            <button
              type="button"
              aria-label="이미지 삭제"
              @click="removeAttachedImage(image.id)"
            >
              <i class="pi pi-times"></i>
            </button>
          </figure>
        </div>
        <p v-if="imageAttachmentError" class="image-attachment-error">
          {{ imageAttachmentError }}
        </p>
      </div>

      <form class="chat-composer" @submit.prevent="sendMessage">
        <input
          ref="fileInputRef"
          class="chat-image-input"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          @change="handleImageInput"
        />
        <input
          v-model="message"
          placeholder="식단이나 운동을 편하게 기록해보세요..."
        />
        <button
          class="attach-image-button"
          type="button"
          aria-label="이미지 추가"
          @click="openImagePicker"
        >
          <i class="pi pi-plus"></i>
        </button>
        <button
          class="send-button"
          type="submit"
          aria-label="전송"
          :disabled="chatStore.isSending"
        >
          <i class="pi pi-send"></i>
        </button>
      </form>

      <p class="composer-note">
        AI 코치는 참고용 가이드를 제공해요. 의학적 진단은 전문가와 상담하세요.
      </p>
    </section>

  </div>
</template>
