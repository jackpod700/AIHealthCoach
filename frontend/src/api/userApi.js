import { apiRequest } from "./apiClient";

export function fetchUserProfile(accessToken) {
  return apiRequest("/api/user/profile", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function patchUserProfile(accessToken, profile) {
  return apiRequest("/api/user/profile", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(profile),
  });
}
