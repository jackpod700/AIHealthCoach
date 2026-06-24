import { apiRequest } from "./apiClient";

export function fetchDailyMeals(accessToken, date) {
  const searchParams = new URLSearchParams({
    date,
  });

  return apiRequest(`/api/meals/daily?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function saveMeal(accessToken, meal) {
  return apiRequest("/api/meals", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(meal),
  });
}

export function deleteMeal(accessToken, mealId) {
  return apiRequest(`/api/meals/${mealId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function searchFoods(accessToken, query) {
  const searchParams = new URLSearchParams({
    query,
  });

  return apiRequest(`/api/foods/search?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function countSearchFoods(accessToken, query) {
  const searchParams = new URLSearchParams({
    query,
  });

  return apiRequest(`/api/foods/search/count?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function fetchMonthlyMeals(accessToken, year, month) {
  const searchParams = new URLSearchParams({
    year: String(year),
    month: String(month),
  });

  return apiRequest(`/api/meals/monthly?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
