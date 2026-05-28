import { defineStore } from "pinia";

export const useHealthStore = defineStore("health", {
  state: () => ({
    userId: Number(localStorage.getItem("ai-health-user-id")) || null,
    accessToken: localStorage.getItem("ai-health-access-token") || "",
    user: JSON.parse(localStorage.getItem("ai-health-user") || "null"),
    isLoading: false,
    isSending: false,
    isLoggingIn: false,
    error: "",
    loginError: "",
    quickPrompts: [
      "아침에 그릭요거트랑 블루베리 먹었어.",
      "점심에 닭가슴살 샐러드 먹었어.",
      "퇴근하고 30분 빠르게 걸었어.",
      "오늘 식단 기준으로 저녁 추천해줘.",
    ],
    messages: [],
    summary: {
      mealCount: 0,
      exerciseCount: 0,
      assistantCount: 0,
    },
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.userId),
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
    async login(credentials) {
      this.isLoggingIn = true;
      this.loginError = "";
      this.error = "";

      try {
        const response = await fetch("/api/user/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(credentials),
        });

        if (!response.ok) {
          throw new Error("이메일 또는 비밀번호를 확인해주세요.");
        }

        const loginResponse = await response.json();

        if (!loginResponse.accessToken) {
          throw new Error("로그인 토큰을 받지 못했습니다.");
        }

        this.userId = loginResponse.userId;
        this.accessToken = loginResponse.accessToken;
        this.user = {
          email: loginResponse.email,
          nickname: loginResponse.nickname,
        };

        localStorage.setItem("ai-health-user-id", String(loginResponse.userId));
        localStorage.setItem("ai-health-access-token", loginResponse.accessToken);
        localStorage.setItem("ai-health-user", JSON.stringify(this.user));

        await this.loadMessages();
      } catch (error) {
        this.loginError = error.message;
      } finally {
        this.isLoggingIn = false;
      }
    },
    logout() {
      this.userId = null;
      this.accessToken = "";
      this.user = null;
      this.messages = [];
      this.error = "";
      this.loginError = "";
      this.refreshSummary();

      localStorage.removeItem("ai-health-user-id");
      localStorage.removeItem("ai-health-access-token");
      localStorage.removeItem("ai-health-user");
    },
    async loadMessages() {
      if (!this.isAuthenticated) {
        return;
      }

      this.isLoading = true;
      this.error = "";

      try {
        const response = await fetch(`/api/chat/messages?userId=${this.userId}`, {
          headers: this.authHeaders(),
        });

        if (!response.ok) {
          throw new Error("채팅 이력을 불러오지 못했습니다.");
        }

        this.messages = await response.json();
        this.refreshSummary();
      } catch (error) {
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },
    async sendMessage(content) {
      const trimmedContent = content.trim();

      if (!trimmedContent || this.isSending || !this.isAuthenticated) {
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
        content: "AI가 답변을 작성하고 있어요...",
        createdAt: new Date(requestedAt.getTime() + 1).toISOString(),
        pending: true,
      };

      this.messages.push(pendingUserMessage, pendingAssistantMessage);
      this.refreshSummary();

      try {
        const response = await fetch("/api/chat/messages", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...this.authHeaders(),
          },
          body: JSON.stringify({
            id: this.userId,
            content: trimmedContent,
          }),
        });

        if (!response.ok) {
          throw new Error("AI 응답을 생성하지 못했습니다.");
        }

        const newMessages = await response.json();
        this.replacePendingMessages(requestId, newMessages);
        this.refreshSummary();
      } catch (error) {
        this.error = error.message;
        this.markPendingMessageFailed(requestId);
        this.refreshSummary();
      } finally {
        this.isSending = false;
      }
    },
    authHeaders() {
      if (!this.accessToken) {
        return {};
      }

      return {
        Authorization: `Bearer ${this.accessToken}`,
      };
    },
    replacePendingMessages(requestId, newMessages) {
      this.messages = this.messages.filter((message) => !message.clientId?.startsWith(requestId));
      this.messages.push(...newMessages);
    },
    markPendingMessageFailed(requestId) {
      const pendingAssistantMessage = this.messages.find((message) => message.clientId === `${requestId}-assistant`);

      if (pendingAssistantMessage) {
        pendingAssistantMessage.content = "응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.";
        pendingAssistantMessage.failed = true;
        pendingAssistantMessage.pending = false;
      }
    },
    refreshSummary() {
      const userMessages = this.messages.filter((message) => message.role === "USER");
      const assistantMessages = this.messages.filter((message) => message.role === "ASSISTANT");

      this.summary = {
        mealCount: userMessages.filter((message) => this.includesAny(message.content, ["먹", "식사", "아침", "점심", "저녁", "샐러드"])).length,
        exerciseCount: userMessages.filter((message) => this.includesAny(message.content, ["운동", "걸", "산책", "헬스", "뛰", "스트레칭"])).length,
        assistantCount: assistantMessages.length,
      };
    },
    includesAny(text = "", keywords = []) {
      return keywords.some((keyword) => text.includes(keyword));
    },
  },
});
