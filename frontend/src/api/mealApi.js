import { apiRequest } from "./apiClient";

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
