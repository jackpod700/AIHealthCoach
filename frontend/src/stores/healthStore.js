import { defineStore } from "pinia";

async function apiRequest(url, options = {}) {
  const response = await fetch(url, options);

  if (response.status === 204) {
    return null;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.error?.message || "요청 처리 중 오류가 발생했습니다.");
  }

  return payload?.data ?? payload;
}

export const useHealthStore = defineStore("health", {
  state: () => ({
    userId: Number(localStorage.getItem("ai-health-user-id")) || null,
    accessToken: localStorage.getItem("ai-health-access-token") || "",
    user: JSON.parse(localStorage.getItem("ai-health-user") || "null"),
    profile: JSON.parse(localStorage.getItem("ai-health-profile") || "null"),
    isLoading: false,
    isSending: false,
    isLoggingIn: false,
    isSigningUp: false,
    isLoadingProfile: false,
    isSavingProfile: false,
    error: "",
    loginError: "",
    signupError: "",
    profileError: "",
    profileSuccess: "",
    quickPrompts: [
      "아침에 그릭요거트랑 블루베리 먹었어",
      "점심에 김치찌개랑 밥 한 공기 먹었어",
      "퇴근하고 30분 빠르게 걸었어",
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
    async signup(credentials) {
      this.isSigningUp = true;
      this.signupError = "";
      this.loginError = "";
      this.error = "";

      try {
        await apiRequest("/api/user/signup", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(credentials),
        });

        await this.login({
          email: credentials.email,
          password: credentials.password,
        });
      } catch (error) {
        this.signupError = error.message;
      } finally {
        this.isSigningUp = false;
      }
    },
    async login(credentials) {
      this.isLoggingIn = true;
      this.loginError = "";
      this.error = "";

      try {
        const loginResponse = await apiRequest("/api/user/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(credentials),
        });

        if (!loginResponse?.accessToken) {
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
      this.profile = null;
      this.messages = [];
      this.error = "";
      this.loginError = "";
      this.signupError = "";
      this.profileError = "";
      this.profileSuccess = "";
      this.refreshSummary();

      localStorage.removeItem("ai-health-user-id");
      localStorage.removeItem("ai-health-access-token");
      localStorage.removeItem("ai-health-user");
      localStorage.removeItem("ai-health-profile");
    },
    async loadProfile() {
      if (!this.isAuthenticated) {
        return;
      }

      this.isLoadingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await apiRequest("/api/user/profile", {
          headers: this.authHeaders(),
        });
        localStorage.setItem("ai-health-profile", JSON.stringify(this.profile));
      } catch (error) {
        this.profileError = error.message;
      } finally {
        this.isLoadingProfile = false;
      }
    },
    async updateProfile(profile) {
      if (!this.isAuthenticated || this.isSavingProfile) {
        return;
      }

      this.isSavingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await apiRequest("/api/user/profile", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            ...this.authHeaders(),
          },
          body: JSON.stringify(profile),
        });
        localStorage.setItem("ai-health-profile", JSON.stringify(this.profile));
        this.profileSuccess = "프로필이 저장되었습니다.";
      } catch (error) {
        this.profileError = error.message;
      } finally {
        this.isSavingProfile = false;
      }
    },
    async loadMessages() {
      if (!this.isAuthenticated) {
        return;
      }

      this.isLoading = true;
      this.error = "";

      try {
        this.messages = await apiRequest("/api/chat/messages", {
          headers: this.authHeaders(),
        });
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
        content: "AI 코치가 답변을 준비하고 있어요...",
        createdAt: new Date(requestedAt.getTime() + 1).toISOString(),
        pending: true,
      };

      this.messages.push(pendingUserMessage, pendingAssistantMessage);
      this.refreshSummary();

      try {
        const response = await apiRequest("/api/chat/messages", {
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

        const newMessages = Array.isArray(response) ? response : response?.messages || [];
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
