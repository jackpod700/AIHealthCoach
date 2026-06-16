import { defineStore } from "pinia";
import { fetchAdminDashboard, fetchAdminDashboardHistory } from "../api/adminApi";
import { useAuthStore } from "./authStore";

export const useAdminStore = defineStore("admin", {
  state: () => ({
    dashboard: null,
    history: null,
    isLoading: false,
    isHistoryLoading: false,
    error: "",
    historyError: "",
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
    async loadDashboardHistory(rangeMinutes = 60) {
      const authStore = useAuthStore();
      this.isHistoryLoading = true;
      this.historyError = "";

      try {
        this.history = await fetchAdminDashboardHistory(authStore.accessToken, rangeMinutes);
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.historyError = error.message;
        }
      } finally {
        this.isHistoryLoading = false;
      }
    },
  },
});
