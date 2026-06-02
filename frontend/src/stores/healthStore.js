import { defineStore } from "pinia";

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
    isConfirmingMealProposal: false,
    error: "",
    loginError: "",
    signupError: "",
    profileError: "",
    profileSuccess: "",
    mealProposal: null,
    mealProposalSelections: [],
    mealProposalQuantities: [],
    mealCalendar: null,
    selectedCalendarMonth: new Date().toISOString().slice(0, 7),
    selectedMealDate: "",
    selectedDailyMeal: null,
    isLoadingMealCalendar: false,
    isLoadingDailyMeal: false,
    mealCalendarError: "",
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
    canConfirmMealProposal: (state) => {
      if (!state.mealProposal?.items?.length) {
        return false;
      }

      return state.mealProposal.items.every((item, index) => {
        const quantity = Number(state.mealProposalQuantities[index]);
        return Boolean(state.mealProposalSelections[index])
          && item.candidates?.length > 0
          && Number.isFinite(quantity)
          && quantity > 0;
      });
    },
  },
  actions: {
    async signup(credentials) {
      this.isSigningUp = true;
      this.signupError = "";
      this.loginError = "";
      this.error = "";

      try {
        const response = await fetch("/api/user/signup", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(credentials),
        });

        if (!response.ok) {
          throw new Error("회원가입에 실패했습니다. 입력 정보를 확인해주세요.");
        }

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

        await Promise.all([
          this.loadMessages(),
          this.loadProfile(),
        ]);
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
      this.mealProposal = null;
      this.mealProposalSelections = [];
      this.mealProposalQuantities = [];
      this.mealCalendar = null;
      this.selectedMealDate = "";
      this.selectedDailyMeal = null;
      this.mealCalendarError = "";
      this.error = "";
      this.loginError = "";
      this.signupError = "";
      this.profileError = "";
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
        const response = await fetch("/api/user/profile", {
          headers: this.authHeaders(),
        });

        if (!response.ok) {
          throw new Error("프로필을 불러오지 못했습니다.");
        }

        this.profile = await response.json();
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

      try {
        const response = await fetch("/api/user/profile", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            ...this.authHeaders(),
          },
          body: JSON.stringify(profile),
        });

        if (!response.ok) {
          throw new Error("프로필을 저장하지 못했습니다.");
        }

        this.profile = await response.json();
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

        const payload = await response.json();
        const newMessages = Array.isArray(payload) ? payload : payload.messages || [];
        this.setMealProposal(Array.isArray(payload) ? null : payload.mealProposal || null);
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
    selectMealCandidate(itemIndex, foodCode) {
      this.mealProposalSelections[itemIndex] = foodCode;
    },
    updateMealProposalQuantity(itemIndex, quantity) {
      this.mealProposalQuantities[itemIndex] = quantity;
    },
    cancelMealProposal() {
      this.mealProposal = null;
      this.mealProposalSelections = [];
      this.mealProposalQuantities = [];
    },
    setMealProposal(proposal) {
      this.mealProposal = proposal;
      this.mealProposalSelections = proposal?.items?.map(() => "") || [];
      this.mealProposalQuantities = proposal?.items?.map((item) => {
        const quantity = Number(item.quantity);
        return Number.isFinite(quantity) && quantity > 0 ? String(quantity) : "1";
      }) || [];
    },
    async confirmMealProposal() {
      if (!this.canConfirmMealProposal || this.isConfirmingMealProposal || !this.isAuthenticated) {
        return;
      }

      this.isConfirmingMealProposal = true;
      this.error = "";

      try {
        const response = await fetch("/api/chat/meal-proposals/confirm", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...this.authHeaders(),
          },
          body: JSON.stringify({
            mealDate: this.mealProposal.mealDate,
            mealType: this.mealProposal.mealType,
            items: this.mealProposal.items.map((item, index) => ({
              foodCode: this.mealProposalSelections[index],
              quantity: Number(this.mealProposalQuantities[index]),
            })),
          }),
        });

        if (!response.ok) {
          throw new Error("식단을 기록하지 못했습니다.");
        }

        const payload = await response.json();
        this.messages.push(...(payload.messages || []));
        await this.refreshMealCalendarAfterMealSave(payload.dailyMeal?.date);
        this.cancelMealProposal();
        this.refreshSummary();
      } catch (error) {
        this.error = error.message;
      } finally {
        this.isConfirmingMealProposal = false;
      }
    },
    async loadMonthlyMeals(year, month) {
      if (!this.isAuthenticated) {
        return;
      }

      this.isLoadingMealCalendar = true;
      this.mealCalendarError = "";
      this.selectedCalendarMonth = `${year}-${String(month).padStart(2, "0")}`;

      try {
        const response = await fetch(`/api/meals/monthly?year=${year}&month=${month}`, {
          headers: this.authHeaders(),
        });

        if (!response.ok) {
          throw new Error("식단 캘린더를 불러오지 못했습니다.");
        }

        this.mealCalendar = await response.json();
      } catch (error) {
        this.mealCalendarError = error.message;
      } finally {
        this.isLoadingMealCalendar = false;
      }
    },
    async loadDailyMeal(date) {
      if (!this.isAuthenticated || !date) {
        return;
      }

      this.isLoadingDailyMeal = true;
      this.mealCalendarError = "";
      this.selectedMealDate = date;

      try {
        const response = await fetch(`/api/meals/daily?date=${date}`, {
          headers: this.authHeaders(),
        });

        if (!response.ok) {
          throw new Error("하루 식단 상세를 불러오지 못했습니다.");
        }

        this.selectedDailyMeal = await response.json();
      } catch (error) {
        this.mealCalendarError = error.message;
      } finally {
        this.isLoadingDailyMeal = false;
      }
    },
    async refreshMealCalendarAfterMealSave(date) {
      if (!date) {
        return;
      }

      const savedMonth = date.slice(0, 7);
      if (this.selectedCalendarMonth === savedMonth) {
        const [year, month] = savedMonth.split("-").map(Number);
        await this.loadMonthlyMeals(year, month);
      }

      if (this.selectedMealDate === date) {
        await this.loadDailyMeal(date);
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
