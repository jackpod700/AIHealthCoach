import { apiRequest } from "./apiClient";

export function fetchAdminDashboard(accessToken) {
  return apiRequest("/api/admin/dashboard", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}
