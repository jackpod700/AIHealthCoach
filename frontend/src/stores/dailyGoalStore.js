import { defineStore } from "pinia";
import { ApiRequestError } from "../api/apiClient";
import {
  confirmDailyGoal,
  fetchDailyGoalProgress,
  fetchDailyGoalRecommendations,
} from "../api/dailyGoalApi";
import { useAuthStore } from "./authStore";

export const useDailyGoalStore = defineStore("dailyGoal", {
  state: () => ({
    recommendation: null,
    recommendations: null,
    currentGoal: null,
    progress: null,
    needsGoalSetup: false,
    isLoadingRecommendation: false,
    isSavingGoal: false,
    isLoadingProgress: false,
    recommendationError: "",
    saveGoalError: "",
    progressError: "",
  }),
  actions: {
    clearDailyGoal() {
      this.recommendation = null;
      this.recommendations = null;
      this.currentGoal = null;
      this.progress = null;
      this.needsGoalSetup = false;
      this.recommendationError = "";
      this.saveGoalError = "";
      this.progressError = "";
    },
    selectRecommendation(goalType) {
      this.recommendation = this.recommendations?.[goalType] ?? null;
      return this.recommendation;
    },
    async loadRecommendations(goalType, { force = false } = {}) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isLoadingRecommendation) {
        return null;
      }

      if (this.recommendations && !force) {
        return this.selectRecommendation(goalType);
      }

      this.isLoadingRecommendation = true;
      this.recommendationError = "";

      try {
        this.recommendations = await fetchDailyGoalRecommendations(
          authStore.accessToken,
        );
        return this.selectRecommendation(goalType);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearDailyGoal();
          return null;
        }

        this.recommendationError = error.message;
        return null;
      } finally {
        this.isLoadingRecommendation = false;
      }
    },
    async saveGoal(goal) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isSavingGoal) {
        return null;
      }

      this.isSavingGoal = true;
      this.saveGoalError = "";

      try {
        this.currentGoal = await confirmDailyGoal(authStore.accessToken, goal);
        this.needsGoalSetup = false;
        return this.currentGoal;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearDailyGoal();
          return null;
        }

        this.saveGoalError = error.message;
        return null;
      } finally {
        this.isSavingGoal = false;
      }
    },
    async loadProgress(date) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return null;
      }

      this.isLoadingProgress = true;
      this.progressError = "";

      try {
        this.progress = await fetchDailyGoalProgress(authStore.accessToken, date);
        this.needsGoalSetup = false;
        return this.progress;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearDailyGoal();
          return null;
        }

        if (isMissingDailyGoal(error)) {
          this.progress = null;
          this.needsGoalSetup = true;
          return null;
        }

        this.progressError = error.message;
        return null;
      } finally {
        this.isLoadingProgress = false;
      }
    },
  },
});

function isMissingDailyGoal(error) {
  return (
    error instanceof ApiRequestError && error.code === "DAILY_GOAL_NOT_FOUND"
  );
}
