import { apiRequest } from "./apiClient";

export function fetchAdminDashboard(accessToken) {
  return apiRequest("/api/admin/dashboard", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function fetchAdminFoodRequests(accessToken, { status = "PENDING", page = 1, size = 20 } = {}) {
  const searchParams = new URLSearchParams({
    status,
    page: String(page),
    size: String(size),
  });

  return apiRequest(`/api/admin/food-requests?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function approveAdminFoodRequest(accessToken, requestId, payload) {
  return apiRequest(`/api/admin/food-requests/${requestId}/approve`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export function rejectAdminFoodRequest(accessToken, requestId, payload) {
  return apiRequest(`/api/admin/food-requests/${requestId}/reject`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export function fetchAdminFoodImportCandidates(accessToken, { status = "PENDING_REVIEW", page = 1, size = 20 } = {}) {
  const searchParams = new URLSearchParams({
    status,
    page: String(page),
    size: String(size),
  });

  return apiRequest(`/api/admin/food-import-candidates?${searchParams.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function approveAdminFoodImportCandidate(accessToken, candidateId) {
  return apiRequest(`/api/admin/food-import-candidates/${candidateId}/approve`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function approveAdminFoodImportCandidates(accessToken, searchMissId, payload) {
  return apiRequest(`/api/admin/food-import-candidates/search-misses/${searchMissId}/approve`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export function rejectAdminFoodImportSearchMiss(accessToken, searchMissId, payload) {
  return apiRequest(`/api/admin/food-import-candidates/search-misses/${searchMissId}/reject`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}
