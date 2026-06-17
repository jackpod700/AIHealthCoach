import { defineStore } from "pinia";
import {
  deleteExerciseRecord,
  fetchDailyExerciseRecords,
  fetchMonthlyExerciseDates,
  saveExerciseRecord,
  searchExerciseActivities,
  updateExerciseRecord,
} from "../api/exerciseApi";
import { useAuthStore } from "./authStore";

const today = new Date();
const todayDateKey = toDateKey(today);

export const useExerciseStore = defineStore("exercise", {
  state: () => ({
    dailyRecords: [],
    monthlyExerciseDates: [],
    activitySearchResults: [],
    selectedDate: todayDateKey,
    selectedYear: today.getFullYear(),
    selectedMonth: today.getMonth() + 1,
    isLoadingDaily: false,
    isLoadingMonthly: false,
    isSearchingActivities: false,
    isSavingRecord: false,
    isDeletingRecord: false,
    dailyError: "",
    monthlyError: "",
    activitySearchError: "",
    saveRecordError: "",
    deleteRecordError: "",
  }),
  getters: {
    monthlyDatesByDate: (state) => {
      return state.monthlyExerciseDates.reduce((acc, date) => {
        acc[date] = true;
        return acc;
      }, {});
    },
    dailyCaloriesBurned: (state) => {
      return state.dailyRecords.reduce((total, record) => {
        return total + toNumber(record.caloriesBurned);
      }, 0);
    },
  },
  actions: {
    async loadDailyExerciseRecords(date = this.selectedDate) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.selectedDate = date;
      this.isLoadingDaily = true;
      this.dailyError = "";

      try {
        this.dailyRecords = await fetchDailyExerciseRecords(authStore.accessToken, date);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.dailyRecords = [];
          return;
        }

        this.dailyError = error.message;
      } finally {
        this.isLoadingDaily = false;
      }
    },
    async searchActivities(keyword) {
      const authStore = useAuthStore();
      const trimmedKeyword = keyword.trim();

      this.activitySearchError = "";

      if (!trimmedKeyword) {
        this.activitySearchResults = [];
        return;
      }

      if (!authStore.isAuthenticated) {
        return;
      }

      this.isSearchingActivities = true;

      try {
        this.activitySearchResults = await searchExerciseActivities(authStore.accessToken, trimmedKeyword);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.activitySearchResults = [];
          return;
        }

        this.activitySearchError = error.message;
      } finally {
        this.isSearchingActivities = false;
      }
    },
    async saveRecord(record) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return false;
      }

      this.isSavingRecord = true;
      this.saveRecordError = "";

      try {
        await saveExerciseRecord(authStore.accessToken, record);
        await this.loadDailyExerciseRecords(record.exerciseDate);
        const [year, month] = record.exerciseDate.split("-").map(Number);
        await this.loadMonthlyExerciseDates(year, month);
        return true;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          return false;
        }

        this.saveRecordError = error.message;
        return false;
      } finally {
        this.isSavingRecord = false;
      }
    },
    async updateRecord(recordId, record) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return false;
      }

      this.isSavingRecord = true;
      this.saveRecordError = "";

      try {
        await updateExerciseRecord(authStore.accessToken, recordId, record);
        await this.loadDailyExerciseRecords(record.exerciseDate);
        const [year, month] = record.exerciseDate.split("-").map(Number);
        await this.loadMonthlyExerciseDates(year, month);
        return true;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          return false;
        }

        this.saveRecordError = error.message;
        return false;
      } finally {
        this.isSavingRecord = false;
      }
    },
    async deleteRecord(recordId, date = this.selectedDate) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isDeletingRecord) {
        return false;
      }

      this.isDeletingRecord = true;
      this.deleteRecordError = "";

      try {
        await deleteExerciseRecord(authStore.accessToken, recordId);
        await this.loadDailyExerciseRecords(date);
        const [year, month] = date.split("-").map(Number);
        await this.loadMonthlyExerciseDates(year, month);
        return true;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          return false;
        }

        this.deleteRecordError = error.message;
        return false;
      } finally {
        this.isDeletingRecord = false;
      }
    },
    async loadMonthlyExerciseDates(year = this.selectedYear, month = this.selectedMonth) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.isLoadingMonthly = true;
      this.monthlyError = "";
      this.selectedYear = year;
      this.selectedMonth = month;

      try {
        this.monthlyExerciseDates = await fetchMonthlyExerciseDates(authStore.accessToken, year, month);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.monthlyExerciseDates = [];
          return;
        }

        this.monthlyError = error.message;
      } finally {
        this.isLoadingMonthly = false;
      }
    },
    clearActivitySearch() {
      this.activitySearchResults = [];
      this.activitySearchError = "";
      this.deleteRecordError = "";
    },
    clearExercise() {
      this.dailyRecords = [];
      this.monthlyExerciseDates = [];
      this.activitySearchResults = [];
      this.dailyError = "";
      this.monthlyError = "";
      this.activitySearchError = "";
      this.saveRecordError = "";
      this.deleteRecordError = "";
    },
  },
});

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function toNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}
