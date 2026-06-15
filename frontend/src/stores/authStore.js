import { defineStore } from "pinia";
import { loginUser, signupUser } from "../api/authApi";
import { isAuthenticationError } from "../api/apiClient";
import { clearAuthSession, loadAuthSession, saveAuthSession } from "../utils/authStorage";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    ...loadAuthSession(),
    isLoggingIn: false,
    isSigningUp: false,
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
