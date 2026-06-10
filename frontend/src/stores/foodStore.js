import { defineStore } from "pinia";
import { fetchFoodGroups } from "../api/foodApi";
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
