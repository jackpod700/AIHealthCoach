import { defineStore } from "pinia";
import { fetchMonthlyMeals } from "../api/mealApi";
import { useAuthStore } from "./authStore";

const today = new Date();

export const useMealStore = defineStore("meal", {
  state: () => ({
    monthlyMeal: null,
    selectedYear: today.getFullYear(),
    selectedMonth: today.getMonth() + 1,
    isLoadingMonthly: false,
    monthlyError: "",
  }),
  getters: {
    monthlyDaysByDate: (state) => {
      const days = state.monthlyMeal?.days || [];

      return days.reduce((acc, day) => {
        acc[day.date] = day;
        return acc;
      }, {});
    },
  },
  actions: {
    setCalendarMonth(year, month) {
      this.selectedYear = year;
      this.selectedMonth = month;
    },
    async loadMonthlyMeals(year = this.selectedYear, month = this.selectedMonth) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.isLoadingMonthly = true;
      this.monthlyError = "";
      this.setCalendarMonth(year, month);

      try {
        this.monthlyMeal = await fetchMonthlyMeals(authStore.accessToken, year, month);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.monthlyMeal = null;
          return;
        }

        this.monthlyError = error.message;
      } finally {
        this.isLoadingMonthly = false;
      }
    },
    clearMeals() {
      this.monthlyMeal = null;
      this.monthlyError = "";
    },
  },
});
