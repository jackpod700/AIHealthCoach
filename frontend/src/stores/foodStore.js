import { defineStore } from "pinia";
import {
  createFoodSubmissionRequest,
  fetchFoodGroups,
  fetchMyFoodSubmissionRequests,
} from "../api/foodApi";
import { useAuthStore } from "./authStore";

export const useFoodStore = defineStore("food", {
  state: () => ({
    foodPage: {
      items: [],
      page: 1,
      size: 20,
      totalItems: 0,
      totalPages: 0,
    },
    query: "",
    page: 1,
    size: 20,
    isLoading: false,
    error: "",
    submissionPage: {
      items: [],
      page: 1,
      size: 20,
      totalItems: 0,
      totalPages: 0,
    },
    isSubmittingFood: false,
    submissionError: "",
    submissionMessage: "",
  }),
  getters: {
    foods: (state) => state.foodPage?.items || [],
    totalItems: (state) => state.foodPage?.totalItems || 0,
    totalPages: (state) => state.foodPage?.totalPages || 0,
  },
  actions: {
    async loadFoodGroups({ query = this.query, page = this.page, size = this.size } = {}) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.query = query;
      this.page = page;
      this.size = size;
      this.isLoading = true;
      this.error = "";

      try {
        this.foodPage = await fetchFoodGroups(authStore.accessToken, {
          query,
          page,
          size,
        });
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearFoods();
          return;
        }

        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },
    async submitMissingFood(payload) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return null;
      }

      this.isSubmittingFood = true;
      this.submissionError = "";
      this.submissionMessage = "";

      try {
        const response = await createFoodSubmissionRequest(authStore.accessToken, payload);
        this.submissionMessage = "등록 요청을 보냈어요. 관리자가 확인한 뒤 음식 DB에 반영됩니다.";
        await this.loadMyFoodSubmissions();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.submissionError = error.message;
        }
        return null;
      } finally {
        this.isSubmittingFood = false;
      }
    },
    async loadMyFoodSubmissions({ page = 1, size = 20 } = {}) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      try {
        this.submissionPage = await fetchMyFoodSubmissionRequests(authStore.accessToken, { page, size });
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.submissionError = error.message;
        }
      }
    },
    clearFoods() {
      this.foodPage = {
        items: [],
        page: 1,
        size: this.size,
        totalItems: 0,
        totalPages: 0,
      };
      this.error = "";
    },
  },
});
