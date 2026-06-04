import { defineStore } from "pinia";
import { fetchDailyMeals, fetchMonthlyMeals, saveMeal, searchFoods } from "../api/mealApi";
import { useAuthStore } from "./authStore";

const today = new Date();
const todayDateKey = toDateKey(today);

export const useMealStore = defineStore("meal", {
  state: () => ({
    dailyMeal: null,
    monthlyMeal: null,
    foodSearchResults: [],
    selectedDate: todayDateKey,
    selectedYear: today.getFullYear(),
    selectedMonth: today.getMonth() + 1,
    isLoadingDaily: false,
    isLoadingMonthly: false,
    isSearchingFoods: false,
    isSavingMeal: false,
    dailyError: "",
    monthlyError: "",
    foodSearchError: "",
    saveMealError: "",
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
    async loadDailyMeal(date = this.selectedDate) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return;
      }

      this.selectedDate = date;
      this.isLoadingDaily = true;
      this.dailyError = "";

      try {
        this.dailyMeal = await fetchDailyMeals(authStore.accessToken, date);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.dailyMeal = null;
          return;
        }

        this.dailyError = error.message;
      } finally {
        this.isLoadingDaily = false;
      }
    },
    async searchMealFoods(query) {
      const authStore = useAuthStore();
      const trimmedQuery = query.trim();

      this.foodSearchError = "";

      if (!trimmedQuery) {
        this.foodSearchResults = [];
        return;
      }

      if (!authStore.isAuthenticated) {
        return;
      }

      this.isSearchingFoods = true;

      try {
        this.foodSearchResults = await searchFoods(authStore.accessToken, trimmedQuery);
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.foodSearchResults = [];
          return;
        }

        this.foodSearchError = error.message;
      } finally {
        this.isSearchingFoods = false;
      }
    },
    async saveMealItems(meal) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return false;
      }

      this.isSavingMeal = true;
      this.saveMealError = "";

      try {
        this.dailyMeal = await saveMeal(authStore.accessToken, meal);
        const [year, month] = meal.mealDate.split("-").map(Number);
        await this.loadMonthlyMeals(year, month);
        return true;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          return false;
        }

        this.saveMealError = error.message;
        return false;
      } finally {
        this.isSavingMeal = false;
      }
    },
    clearFoodSearch() {
      this.foodSearchResults = [];
      this.foodSearchError = "";
    },
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
      this.dailyMeal = null;
      this.monthlyMeal = null;
      this.foodSearchResults = [];
      this.dailyError = "";
      this.monthlyError = "";
      this.foodSearchError = "";
      this.saveMealError = "";
    },
  },
});

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
