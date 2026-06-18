import { defineStore } from "pinia";
import {
  fetchUserProfile,
  patchUserProfile,
  updateUserNickname,
} from "../api/userApi";
import { clearStoredProfile, loadStoredProfile, saveStoredProfile } from "../utils/authStorage";
import { useAuthStore } from "./authStore";

export const useProfileStore = defineStore("profile", {
  state: () => ({
    profile: loadStoredProfile(),
    isLoadingProfile: false,
    isSavingProfile: false,
    profileError: "",
    profileSuccess: "",
  }),

  actions: {
    clearProfile() {
      this.profile = null;
      this.profileError = "";
      this.profileSuccess = "";
      clearStoredProfile();
    },

    async loadProfile() {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return null;
      }

      this.isLoadingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await fetchUserProfile(authStore.accessToken);
        saveStoredProfile(this.profile);
        return this.profile;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearProfile();
          return null;
        }

        this.profileError = error.message;
        throw error;
      } finally {
        this.isLoadingProfile = false;
      }
    },

    async updateProfile(profile) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isSavingProfile) {
        return null;
      }

      this.isSavingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await patchUserProfile(authStore.accessToken, profile);
        saveStoredProfile(this.profile);
        this.profileSuccess = "프로필이 저장되었습니다.";
        return this.profile;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearProfile();
          return null;
        }

        this.profileError = error.message;
        throw error;
      } finally {
        this.isSavingProfile = false;
      }
    },

    async updateNickname(nickname) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.profileError = "";
      this.profileSuccess = "";

      try {
        await updateUserNickname(authStore.accessToken, nickname);
        authStore.updateUser({ nickname });
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearProfile();
          return;
        }

        this.profileError = error.message;
        throw error;
      }
    },
  },
});