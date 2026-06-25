import { defineStore } from "pinia";
import {
  approveAdminFoodImportCandidates,
  approveAdminFoodRequest,
  fetchAdminDashboard,
  fetchAdminFoodImportCandidates,
  fetchAdminFoodRequests,
  rejectAdminFoodImportSearchMiss,
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
    importCandidatePage: {
      items: [],
      page: 1,
      size: 20,
      totalItems: 0,
      totalPages: 0,
    },
    importCandidateStatus: "PENDING_REVIEW",
    isLoadingImportCandidates: false,
    importCandidateError: "",
    importCandidateMessage: "",
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
        await this.loadFoodRequests();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.foodRequestError = error.message;
        }
        return null;
      }
    },
    async loadImportCandidates({ status = this.importCandidateStatus, page = 1, size = 20 } = {}) {
      const authStore = useAuthStore();
      this.isLoadingImportCandidates = true;
      this.importCandidateError = "";
      this.importCandidateStatus = status;

      try {
        this.importCandidatePage = await fetchAdminFoodImportCandidates(authStore.accessToken, { status, page, size });
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.importCandidateError = error.message;
        }
      } finally {
        this.isLoadingImportCandidates = false;
      }
    },
    async approveImportCandidates(searchMissId, candidateIds) {
      const authStore = useAuthStore();
      this.importCandidateError = "";
      this.importCandidateMessage = "";

      try {
        const response = await approveAdminFoodImportCandidates(authStore.accessToken, searchMissId, { candidateIds });
        this.importCandidateMessage = "FatSecret 후보를 승인했습니다.";
        await this.loadImportCandidates();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.importCandidateError = error.message;
        }
        return null;
      }
    },
    async rejectImportSearchMiss(searchMissId, rejectionReason) {
      const authStore = useAuthStore();
      this.importCandidateError = "";
      this.importCandidateMessage = "";

      try {
        const response = await rejectAdminFoodImportSearchMiss(authStore.accessToken, searchMissId, { rejectionReason });
        this.importCandidateMessage = "검색어 후보를 전체 거절했습니다.";
        await this.loadImportCandidates();
        return response;
      } catch (error) {
        if (!authStore.handleAuthFailure(error)) {
          this.importCandidateError = error.message;
        }
        return null;
      }
    },
  },
});
