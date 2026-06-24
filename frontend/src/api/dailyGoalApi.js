import { apiRequest } from "./apiClient";

export function fetchDailyGoalRecommendations(accessToken) {
  return apiRequest("/api/daily-goals/recommendations", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function confirmDailyGoal(accessToken, goal) {
  return apiRequest("/api/daily-goals/confirm", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(goal),
  });
}

export function fetchDailyGoalProgress(accessToken, date) {
  const searchParams = new URLSearchParams({
    date,
  });

  return apiRequest(`/api/daily-goals/progress?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
