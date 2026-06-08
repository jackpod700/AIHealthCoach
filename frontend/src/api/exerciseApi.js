import { apiRequest } from "./apiClient";

export function searchExerciseActivities(accessToken, keyword) {
  const searchParams = new URLSearchParams({
    keyword,
  });

  return apiRequest(`/api/exercise/physical-activities?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function saveExerciseRecord(accessToken, record) {
  return apiRequest("/api/exercise/records", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(record),
  });
}

export function updateExerciseRecord(accessToken, recordId, record) {
  return apiRequest(`/api/exercise/records/${recordId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(record),
  });
}

export function fetchDailyExerciseRecords(accessToken, date) {
  const searchParams = new URLSearchParams({
    date,
  });

  return apiRequest(`/api/exercise/records?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function fetchMonthlyExerciseDates(accessToken, year, month) {
  const searchParams = new URLSearchParams({
    year: String(year),
    month: String(month),
  });

  return apiRequest(`/api/exercise/records/calendar?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
