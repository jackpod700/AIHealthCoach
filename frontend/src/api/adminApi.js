import { apiRequest } from "./apiClient";

export function fetchAdminDashboard(accessToken) {
  return apiRequest("/api/admin/dashboard", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function fetchAdminDashboardHistory(accessToken, rangeMinutes = 60) {
  return apiRequest(`/api/admin/dashboard/history?rangeMinutes=${rangeMinutes}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
