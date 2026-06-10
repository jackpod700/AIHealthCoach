import { apiRequest } from "./apiClient";

export function fetchFoodGroups(accessToken, { query = "", page = 1, size = 20 } = {}) {
  const searchParams = new URLSearchParams({
    query,
    page: String(page),
    size: String(size),
  });

  return apiRequest(`/api/foods?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
