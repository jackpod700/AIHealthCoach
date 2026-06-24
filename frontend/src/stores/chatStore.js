import { defineStore } from "pinia";
import {
  confirmMealProposal,
  fetchChatMessages,
  postChatImageMessage,
  postChatMessageStream,
} from "../api/chatApi";
import { useAuthStore } from "./authStore";

const STREAM_REVEAL_CHARS_PER_TICK = 12;
const STREAM_REVEAL_INTERVAL_MS = 16;

export const useChatStore = defineStore("chat", {
  state: () => ({
    messages: [],
    mealProposal: null,
    exerciseProposal: null,
    weightProposal: null,
    isLoading: false,
    isSending: false,
    isConfirmingMeal: false,
    isConfirmingExercise: false,
    isConfirmingWeight: false,
    error: "",
    mealProposalError: "",
    exerciseProposalError: "",
    weightProposalError: "",
    summary: {
      mealCount: 0,
      exerciseCount: 0,
      assistantCount: 0,
    },
  }),
  getters: {
    orderedMessages: (state) => {
      return [...state.messages].sort((a, b) => {
        if (Number.isFinite(a.clientOrder) && Number.isFinite(b.clientOrder)) {
          return a.clientOrder - b.clientOrder;
        }

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
      this.weightProposal = null;
      this.error = "";
      this.mealProposalError = "";
      this.exerciseProposalError = "";
      this.weightProposalError = "";
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
      const requestOrder = requestedAt.getTime() * 2;
      const pendingUserMessage = {
        clientId: `${requestId}-user`,
        clientOrder: requestOrder,
        role: "USER",
        content: trimmedContent,
        createdAt: requestedAt.toISOString(),
        pending: true,
      };
      const pendingAssistantMessage = {
        clientId: `${requestId}-assistant`,
        clientOrder: requestOrder + 1,
        role: "ASSISTANT",
        content: "",
        createdAt: new Date(requestedAt.getTime() + 1).toISOString(),
        pending: true,
      };

      this.messages.push(pendingUserMessage, pendingAssistantMessage);
      this.refreshSummary();

      let savedAssistantMessage = null;
      let pendingToolResult = null;
      const streamDebug = createChatStreamDebugLogger(requestId);
      const deltaRevealer = createDeltaRevealer((content) => {
        this.appendAssistantDelta(requestId, content);
      });

      try {
        streamDebug.log("request_start");
        await postChatMessageStream(authStore.accessToken, trimmedContent, {
          delta: (event) => {
            streamDebug.logDelta(event?.content || "");
            deltaRevealer.enqueue(event?.content || "");
          },
          assistant_done: (event) => {
            streamDebug.log("assistant_done", {
              contentLength: event?.message?.content?.length || 0,
            });
            savedAssistantMessage = event?.message || null;
            this.completeStreamingAssistant(requestId, savedAssistantMessage);
            streamDebug.log("assistant_done_applied");
          },
          tool_result: (event) => {
            streamDebug.log("tool_result", {
              status: event?.status,
              hasMeal: Boolean(event?.mealProposal?.items?.length),
              hasExercise: Boolean(event?.exerciseProposal?.activityKeyword),
              hasWeight: Boolean(event?.weightProposal?.weightKg),
            });
            pendingToolResult = event;
          },
          error: (event) => {
            streamDebug.log("stream_error", {
              code: event?.code,
              message: event?.message,
            });
            throw new Error(event?.message || "답변 생성에 실패했습니다.");
          },
        });
        streamDebug.log("stream_done");
        await deltaRevealer.flush();
        streamDebug.log("flush_done");
        this.completeStreamingMessages(requestId, savedAssistantMessage);
        this.applyToolResult(pendingToolResult);
        streamDebug.log("proposal_applied");
        this.refreshSummary();
      } catch (error) {
        await deltaRevealer.flush();
        streamDebug.log("failed", {
          message: error.message,
        });
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
        this.weightProposal = response?.weightProposal?.weightKg ? response.weightProposal : null;
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
    startConfirmingWeight() {
      if (this.isConfirmingWeight) {
        return false;
      }

      this.isConfirmingWeight = true;
      this.weightProposalError = "";
      return true;
    },
    finishConfirmingWeight() {
      this.isConfirmingWeight = false;
    },
    completeWeightProposal() {
      this.weightProposal = null;
      this.weightProposalError = "";
      this.refreshSummary();
    },
    failWeightProposal(message) {
      this.weightProposalError = message;
    },
    dismissMealProposal() {
      this.mealProposal = null;
      this.mealProposalError = "";
    },
    dismissExerciseProposal() {
      this.exerciseProposal = null;
      this.exerciseProposalError = "";
    },
    dismissWeightProposal() {
      this.weightProposal = null;
      this.weightProposalError = "";
    },
    replacePendingMessages(requestId, newMessages) {
      this.messages = this.messages.filter((message) => !message.clientId?.startsWith(requestId));
      this.messages.push(...newMessages);
    },
    markStreamingUserSaved(requestId) {
      this.messages = this.messages.map((message) => {
        if (message.clientId !== `${requestId}-user`) {
          return message;
        }

        return {
          ...message,
          pending: false,
        };
      });
    },
    appendAssistantDelta(requestId, content) {
      const pendingAssistantMessage = this.messages.find((message) => message.clientId === `${requestId}-assistant`);

      if (pendingAssistantMessage) {
        pendingAssistantMessage.content += content;
      }
    },
    completeStreamingAssistant(requestId, assistantMessage) {
      this.messages = this.messages.map((message) => {
        if (message.clientId === `${requestId}-user`) {
          return {
            ...message,
            pending: false,
          };
        }

        if (message.clientId === `${requestId}-assistant`) {
          if (!assistantMessage) {
            return {
              ...message,
              pending: false,
            };
          }

          return {
            ...assistantMessage,
            clientId: message.clientId,
            clientOrder: message.clientOrder,
            content: message.content || assistantMessage.content,
            createdAt: message.createdAt,
            pending: false,
          };
        }

        return message;
      });
    },
    completeStreamingMessages(requestId, assistantMessage) {
      this.messages = this.messages.map((message) => {
        if (message.clientId === `${requestId}-user`) {
          return {
            ...message,
            pending: false,
          };
        }

        if (message.clientId === `${requestId}-assistant`) {
          if (!assistantMessage) {
            return {
              ...message,
              pending: false,
            };
          }

          return {
            ...assistantMessage,
            clientId: message.clientId,
            clientOrder: message.clientOrder,
            content: message.content || assistantMessage.content,
            createdAt: message.createdAt,
            pending: false,
          };
        }

        return message;
      });
    },
    applyToolResult(toolResult) {
      if (!toolResult || toolResult.status !== "SUCCESS") {
        return;
      }

      this.mealProposal = toolResult.mealProposal?.items?.length ? toolResult.mealProposal : null;
      this.exerciseProposal = toolResult.exerciseProposal?.activityKeyword ? toolResult.exerciseProposal : null;
      this.weightProposal = toolResult.weightProposal?.weightKg ? toolResult.weightProposal : null;
    },
    markPendingMessageFailed(requestId) {
      const pendingAssistantMessage = this.messages.find((message) => message.clientId === `${requestId}-assistant`);

      if (pendingAssistantMessage) {
        if (!pendingAssistantMessage.content?.trim()) {
          pendingAssistantMessage.content = "답변 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        }
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

function createChatStreamDebugLogger(requestId) {
  const enabled = import.meta.env.DEV;
  const startedAt = performance.now();
  let deltaCount = 0;
  let deltaChars = 0;
  let firstDeltaLogged = false;

  function log(eventName, details = {}) {
    if (!enabled) {
      return;
    }

    console.debug("[chat-stream]", {
      requestId,
      event: eventName,
      elapsedMs: Math.round(performance.now() - startedAt),
      ...details,
    });
  }

  function logDelta(content) {
    deltaCount += 1;
    deltaChars += content.length;

    if (!firstDeltaLogged) {
      firstDeltaLogged = true;
      log("first_delta", {
        deltaCount,
        deltaChars,
      });
      return;
    }

    if (deltaCount % 20 === 0) {
      log("delta_progress", {
        deltaCount,
        deltaChars,
      });
    }
  }

  return {
    log,
    logDelta,
  };
}

function createDeltaRevealer(onReveal) {
  let queue = "";
  let timerId = null;
  let stopped = false;
  let idleResolvers = [];

  function enqueue(content) {
    if (!content || stopped) {
      return;
    }

    queue += content;

    if (!timerId) {
      revealNext();
    }
  }

  function revealNext() {
    if (stopped) {
      return;
    }

    const nextContent = queue.slice(0, STREAM_REVEAL_CHARS_PER_TICK);
    queue = queue.slice(STREAM_REVEAL_CHARS_PER_TICK);

    if (nextContent) {
      onReveal(nextContent);
    }

    if (queue) {
      timerId = window.setTimeout(revealNext, STREAM_REVEAL_INTERVAL_MS);
      return;
    }

    timerId = null;
    idleResolvers.splice(0).forEach((resolve) => resolve());
  }

  function flush() {
    if (!queue && !timerId) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      idleResolvers.push(resolve);
    });
  }

  function stop() {
    stopped = true;
    queue = "";

    if (timerId) {
      window.clearTimeout(timerId);
      timerId = null;
    }

    idleResolvers.splice(0).forEach((resolve) => resolve());
  }

  return {
    enqueue,
    flush,
    stop,
  };
}
