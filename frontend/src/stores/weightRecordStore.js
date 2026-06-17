import { defineStore } from "pinia";
import { deleteWeightRecord, fetchWeightRecords, saveWeightRecord } from "../api/weightRecordApi";
import { useAuthStore } from "./authStore";

const DEFAULT_RANGE = "30";

export const useWeightRecordStore = defineStore("weightRecord", {
  state: () => ({
    records: [],
    calendarRecords: [],
    selectedRange: DEFAULT_RANGE,
    isLoadingRecords: false,
    isLoadingCalendarRecords: false,
    isSavingRecord: false,
    isDeletingRecord: false,
    loadError: "",
    calendarError: "",
    saveError: "",
    deleteError: "",
  }),
  getters: {
    recordsByDate: (state) => {
      return state.records.reduce((acc, record) => {
        acc[record.recordDate] = record;
        return acc;
      }, {});
    },
    calendarRecordsByDate: (state) => {
      return state.calendarRecords.reduce((acc, record) => {
        acc[record.recordDate] = record;
        return acc;
      }, {});
    },
    latestRecord: (state) => {
      return state.records.at(-1) || null;
    },
  },
  actions: {
    async loadRecords(range = this.selectedRange) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return [];
      }

      this.selectedRange = range;
      this.isLoadingRecords = true;
      this.loadError = "";

      try {
        this.records = await fetchWeightRecords(authStore.accessToken, rangeToDateParams(range));
        return this.records;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearWeightRecords();
          return [];
        }

        this.loadError = error.message;
        return [];
      } finally {
        this.isLoadingRecords = false;
      }
    },
    async saveRecord(record) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isSavingRecord) {
        return null;
      }

      this.isSavingRecord = true;
      this.saveError = "";

      try {
        const savedRecord = await saveWeightRecord(authStore.accessToken, record);
        await this.loadRecords(this.selectedRange);
        return savedRecord;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearWeightRecords();
          return null;
        }

        this.saveError = error.message;
        return null;
      } finally {
        this.isSavingRecord = false;
      }
    },
    async loadCalendarRecords(year, month) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated) {
        return [];
      }

      this.isLoadingCalendarRecords = true;
      this.calendarError = "";

      try {
        this.calendarRecords = await fetchWeightRecords(authStore.accessToken, monthToDateParams(year, month));
        return this.calendarRecords;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearWeightRecords();
          return [];
        }

        this.calendarError = error.message;
        return [];
      } finally {
        this.isLoadingCalendarRecords = false;
      }
    },
    async deleteRecord(recordDate) {
      const authStore = useAuthStore();

      if (!authStore.isAuthenticated || this.isDeletingRecord) {
        return false;
      }

      this.isDeletingRecord = true;
      this.deleteError = "";

      try {
        await deleteWeightRecord(authStore.accessToken, recordDate);
        await this.loadRecords(this.selectedRange);
        return true;
      } catch (error) {
        if (authStore.handleAuthFailure(error)) {
          this.clearWeightRecords();
          return false;
        }

        this.deleteError = error.message;
        return false;
      } finally {
        this.isDeletingRecord = false;
      }
    },
    clearWeightRecords() {
      this.records = [];
      this.calendarRecords = [];
      this.loadError = "";
      this.calendarError = "";
      this.saveError = "";
      this.deleteError = "";
    },
  },
});

function rangeToDateParams(range) {
  if (range === "all") {
    return {};
  }

  const days = Number(range);

  if (!Number.isFinite(days)) {
    return {};
  }

  const to = new Date();
  const from = new Date(to);
  from.setDate(to.getDate() - days + 1);

  return {
    from: toDateKey(from),
    to: toDateKey(to),
  };
}

function monthToDateParams(year, month) {
  const from = new Date(year, month - 1, 1);
  const to = new Date(year, month, 0);

  return {
    from: toDateKey(from),
    to: toDateKey(to),
  };
}

function toDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}
