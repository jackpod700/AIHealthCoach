import { defineStore } from "pinia";
import { confirmMealProposal, fetchChatMessages, postChatImageMessage, postChatMessage } from "../api/chatApi";
import { useAuthStore } from "./authStore";

export const useChatStore = defineStore("chat", {
  state: () => ({
    messages: [],
    mealProposal: null,
    exerciseProposal: null,
    isLoading: false,
    isSending: false,
    isConfirmingMeal: false,
    isConfirmingExercise: false,
    error: "",
    mealProposalError: "",
    exerciseProposalError: "",
    summary: {
      mealCount: 0,
      exerciseCount: 0,
      assistantCount: 0,
    },
  }),
  getters: {
    orderedMessages: (state) => {
      return [...state.messages].sort((a, b) => {
        return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      });
    },
    lastAssistantMessage: (state) => {
      return [...state.messages].reverse().find((message) => message.role === "ASSISTANT");
    },
  },
  actions: {
    clearMessages() {
      this.messages = [];
      this.mealProposal = null;
      this.exerciseProposal = null;
      this.error = "";
      this.mealProposalError = "";
      this.exerciseProposalError = "";
      this.refreshSummary();
    },
    async loadMessages() {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.isLoading = true;
      this.error = "";

      try {
        this.messages = await fetchChatMessages(authStore.accessToken);
        this.refreshSummary();
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearMessages();
          return;
        }

        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },
    async sendMessage(content) {
      const authStore = useAuthStore();
      const trimmedContent = content.trim();

      if (!trimmedContent || this.isSending || !authStore.isAuthenticated) {
        return;
      }

      this.isSending = true;
      this.error = "";

      const requestedAt = new Date();
      const requestId = `pending-${requestedAt.getTime()}`;
      const pendingUserMessage = {
        clientId: `${requestId}-user`,
        role: "USER",
        content: trimmedContent,
        createdAt: requestedAt.toISOString(),
        pending: true,
      };
      const pendingAssistantMessage = {
        clientId: `${requestId}-assistant`,
        role: "ASSISTANT",
        content: "AI 코치가 답변을 준비하고 있어요...",
        createdAt: new Date(requestedAt.getTime() + 1).toISOString(),
        pending: true,
      };

      this.messages.push(pendingUserMessage, pendingAssistantMessage);
      this.refreshSummary();

      try {
        const response = await postChatMessage(authStore.accessToken, authStore.userId, trimmedContent);
        const newMessages = Array.isArray(response) ? response : response?.messages || [];
        this.replacePendingMessages(requestId, newMessages);
        this.mealProposal = response?.mealProposal?.items?.length ? response.mealProposal : null;
        this.exerciseProposal = response?.exerciseProposal?.activityKeyword ? response.exerciseProposal : null;
        this.refreshSummary();
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearMessages();
          return;
        }

        this.error = error.message;
        this.markPendingMessageFailed(requestId);
        this.refreshSummary();
      } finally {
        this.isSending = false;
      }
    },
    async sendImageMessage(content, images = []) {
      const authStore = useAuthStore();
      const trimmedContent = content.trim();

      if (!images.length || this.isSending || !authStore.isAuthenticated) {
        return;
      }

      this.isSending = true;
      this.error = "";

      const requestedAt = new Date();
      const requestId = `pending-${requestedAt.getTime()}`;
      const userSummary = `사진 ${images.length}장을 업로드했습니다.${trimmedContent ? `\n${trimmedContent}` : ""}`;
      const pendingUserMessage = {
        clientId: `${requestId}-user`,
        role: "USER",
        content: userSummary,
        createdAt: requestedAt.toISOString(),
        pending: true,
      };
      const pendingAssistantMessage = {
        clientId: `${requestId}-assistant`,
        role: "ASSISTANT",
        content: "사진을 분석하고 있어요...",
        createdAt: new Date(requestedAt.getTime() + 1).toISOString(),
        pending: true,
      };

      this.messages.push(pendingUserMessage, pendingAssistantMessage);
      this.refreshSummary();

      try {
        const response = await postChatImageMessage(authStore.accessToken, trimmedContent, images);
        const newMessages = Array.isArray(response) ? response : response?.messages || [];
        this.replacePendingMessages(requestId, newMessages);
        this.mealProposal = response?.mealProposal?.items?.length ? response.mealProposal : null;
        this.exerciseProposal = response?.exerciseProposal?.activityKeyword ? response.exerciseProposal : null;
        this.refreshSummary();
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearMessages();
          return;
        }

        this.error = error.message;
        this.markPendingMessageFailed(requestId);
        this.refreshSummary();
      } finally {
        this.isSending = false;
      }
    },
    async confirmMealProposal(payload) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isConfirmingMeal) {
        return null;
      }

      this.isConfirmingMeal = true;
      this.mealProposalError = "";

      try {
        const response = await confirmMealProposal(authStore.accessToken, payload);
        const newMessages = response?.messages || [];
        this.messages.push(...newMessages);
        this.mealProposal = null;
        this.refreshSummary();
        return response;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearMessages();
          return null;
        }

        this.mealProposalError = error.message;
        return null;
      } finally {
        this.isConfirmingMeal = false;
      }
    },
    startConfirmingExercise() {
      if (this.isConfirmingExercise) {
        return false;
      }

      this.isConfirmingExercise = true;
      this.exerciseProposalError = "";
      return true;
    },
    finishConfirmingExercise() {
      this.isConfirmingExercise = false;
    },
    completeExerciseProposal() {
      this.exerciseProposal = null;
      this.exerciseProposalError = "";
      this.refreshSummary();
    },
    failExerciseProposal(message) {
      this.exerciseProposalError = message;
    },
    dismissMealProposal() {
      this.mealProposal = null;
      this.mealProposalError = "";
    },
    dismissExerciseProposal() {
      this.exerciseProposal = null;
      this.exerciseProposalError = "";
    },
    replacePendingMessages(requestId, newMessages) {
      this.messages = this.messages.filter((message) => !message.clientId?.startsWith(requestId));
      this.messages.push(...newMessages);
    },
    markPendingMessageFailed(requestId) {
      const pendingAssistantMessage = this.messages.find((message) => message.clientId === `${requestId}-assistant`);

      if (pendingAssistantMessage) {
        pendingAssistantMessage.content = "답변 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        pendingAssistantMessage.failed = true;
        pendingAssistantMessage.pending = false;
      }
    },
    refreshSummary() {
      const userMessages = this.messages.filter((message) => message.role === "USER");
      const assistantMessages = this.messages.filter((message) => message.role === "ASSISTANT");

      this.summary = {
        mealCount: userMessages.filter((message) => this.includesAny(message.content, ["먹", "식사", "아침", "점심", "저녁", "간식"])).length,
        exerciseCount: userMessages.filter((message) => this.includesAny(message.content, ["운동", "걷", "러닝", "헬스", "스트레칭"])).length,
        assistantCount: assistantMessages.length,
      };
    },
    includesAny(text = "", keywords = []) {
      return keywords.some((keyword) => text.includes(keyword));
    },
  },
});
