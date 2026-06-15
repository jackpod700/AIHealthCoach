import { defineStore } from "pinia";
import { fetchAdminDashboard } from "../api/adminApi";
import { useAuthStore } from "./authStore";

export const useAdminStore = defineStore("admin", {
  state: () => ({
    dashboard: null,
    isLoading: false,
    error: "",
  }),
  actions: {
    async loadDashboard() {
      const authStore = useAuthStore();
      this.isLoading = true;
      this.error = "";

      try {
        this.dashboard = await fetchAdminDashboard(authStore.accessToken);
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.error = error.message;
        }
      } finally {
        this.isLoading = false;
      }
    },
  },
});
