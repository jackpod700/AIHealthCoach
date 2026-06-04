import { defineStore } from "pinia";
import { fetchUserProfile, patchUserProfile } from "../api/userApi";
import { useAuthStore } from "./authStore";
import { clearStoredProfile, loadStoredProfile, saveStoredProfile } from "../utils/authStorage";

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
        return;
      }

      this.isLoadingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await fetchUserProfile(authStore.accessToken);
        saveStoredProfile(this.profile);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearProfile();
          return;
        }

        this.profileError = error.message;
      } finally {
        this.isLoadingProfile = false;
      }
    },
    async updateProfile(profile) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isSavingProfile) {
        return;
      }

      this.isSavingProfile = true;
      this.profileError = "";
      this.profileSuccess = "";

      try {
        this.profile = await patchUserProfile(authStore.accessToken, profile);
        saveStoredProfile(this.profile);
        this.profileSuccess = "프로필이 저장되었습니다.";
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearProfile();
          return;
        }

        this.profileError = error.message;
      } finally {
        this.isSavingProfile = false;
      }
    },
  },
});
