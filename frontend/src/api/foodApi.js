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

export function createFoodSubmissionRequest(accessToken, payload) {
  return apiRequest("/api/foods/requests", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export function fetchMyFoodSubmissionRequests(accessToken, { page = 1, size = 20 } = {}) {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  return apiRequest(`/api/foods/requests/my?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
