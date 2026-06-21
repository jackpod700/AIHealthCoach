import { defineStore } from "pinia";
import { loginUser, refreshAccessToken, signupUser } from "../api/authApi";
import { isAuthenticationError } from "../api/apiClient";
import { fetchCurrentUser } from "../api/userApi";
import { clearAuthSession, loadAuthSession, saveAuthSession } from "../utils/authStorage";

function decodeJwtPayload(token) {
  if (!token) {
    return null;
  }

  const [, payload] = token.split(".");

  if (!payload) {
    return null;
  }

  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decodedPayload = atob(base64);
    return JSON.parse(decodedPayload);
  } catch {
    return null;
  }
}

function extractUserIdFromAccessToken(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  const subject = payload?.sub;

  if (!subject) {
    return null;
  }

  const userId = Number(subject);

  return Number.isNaN(userId) ? null : userId;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    ...loadAuthSession(),
    isLoggingIn: false,
    isSigningUp: false,
    isOAuthLoggingIn: false,
    loginError: "",
    signupError: "",
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.userId),
    isAdmin: (state) => state.user?.role === "ADMIN",
  },

  actions: {
    async signup(credentials) {
      this.isSigningUp = true;
      this.signupError = "";
      this.loginError = "";

      try {
        await signupUser(credentials);

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

      try {
        const loginResponse = await loginUser(credentials);

        if (!loginResponse?.accessToken) {
          throw new Error("로그인 토큰을 받지 못했습니다.");
        }

        this.userId = loginResponse.userId;
        this.accessToken = loginResponse.accessToken;
        this.user = {
          email: loginResponse.email,
          nickname: loginResponse.nickname,
          role: loginResponse.role || "USER",
        };

        saveAuthSession({
          userId: this.userId,
          accessToken: this.accessToken,
          user: this.user,
        });
      } catch (error) {
        this.loginError = error.message;
      } finally {
        this.isLoggingIn = false;
      }
    },

    async completeOAuthLogin() {
      this.isOAuthLoggingIn = true;
      this.loginError = "";
      this.signupError = "";

      try {
        const refreshResponse = await refreshAccessToken();

        if (!refreshResponse?.accessToken) {
          throw new Error("소셜 로그인 토큰을 받지 못했습니다.");
        }

        const userId = extractUserIdFromAccessToken(refreshResponse.accessToken);

        if (!userId) {
          throw new Error("소셜 로그인 사용자 정보를 확인하지 못했습니다.");
        }

        this.userId = userId;
        this.accessToken = refreshResponse.accessToken;

        const currentUser = await fetchCurrentUser(this.accessToken);

        this.user = {
          email: currentUser.email,
          nickname: currentUser.nickname,
          role: currentUser.role || "USER",
        };

        saveAuthSession({
          userId: this.userId,
          accessToken: this.accessToken,
          user: this.user,
        });
      } catch (error) {
        this.logout(error.message || "소셜 로그인 처리 중 오류가 발생했습니다.");
        throw error;
      } finally {
        this.isOAuthLoggingIn = false;
      }
    },

    updateUser(user) {
      this.user = {
        ...(this.user || {}),
        ...user,
      };

      saveAuthSession({
        userId: this.userId,
        accessToken: this.accessToken,
        user: this.user,
      });
    },

    logout(message = "") {
      this.userId = null;
      this.accessToken = "";
      this.user = null;
      this.loginError = message;
      this.signupError = "";
      clearAuthSession();
    },

    handleAuthFailure(error) {
      if (!isAuthenticationError(error)) {
        return false;
      }

      this.logout(error.message || "로그인이 필요합니다.");
      return true;
    },
  },
});
