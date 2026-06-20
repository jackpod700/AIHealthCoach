import { defineStore } from "pinia";
import {
  approveAdminFoodRequest,
  fetchAdminDashboard,
  fetchAdminFoodRequests,
  rejectAdminFoodRequest,
} from "../api/adminApi";
import { useAuthStore } from "./authStore";

export const useAdminStore = defineStore("admin", {
  state: () => ({
    dashboard: null,
    isLoading: false,
    error: "",
    foodRequestPage: {
      items: [],
      page: 1,
      size: 20,
      totalItems: 0,
      totalPages: 0,
    },
    isLoadingFoodRequests: false,
    foodRequestError: "",
    foodRequestMessage: "",
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
    async loadFoodRequests({ status = "PENDING", page = 1, size = 20 } = {}) {
      const authStore = useAuthStore();
      this.isLoadingFoodRequests = true;
      this.foodRequestError = "";

      try {
        this.foodRequestPage = await fetchAdminFoodRequests(authStore.accessToken, { status, page, size });
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.foodRequestError = error.message;
        }
      } finally {
        this.isLoadingFoodRequests = false;
      }
    },
    async approveFoodRequest(requestId, payload) {
      const authStore = useAuthStore();
      this.foodRequestError = "";
      this.foodRequestMessage = "";

      try {
        const response = await approveAdminFoodRequest(authStore.accessToken, requestId, payload);
        this.foodRequestMessage = "Food request approved.";
        await this.loadFoodRequests();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.foodRequestError = error.message;
        }
        return null;
      }
    },
    async rejectFoodRequest(requestId, rejectionReason) {
      const authStore = useAuthStore();
      this.foodRequestError = "";
      this.foodRequestMessage = "";

      try {
        const response = await rejectAdminFoodRequest(authStore.accessToken, requestId, { rejectionReason });
        this.foodRequestMessage = "Food request rejected.";
        await this.loadFoodRequests();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.foodRequestError = error.message;
        }
        return null;
      }
    },
  },
});
