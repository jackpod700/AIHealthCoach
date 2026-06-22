import { apiRequest } from "./apiClient";

export function fetchCurrentUser(accessToken) {
  return apiRequest("/api/user/me", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function updateUserNickname(accessToken, nickname) {
  return apiRequest("/api/user/nickname", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ nickname }),
  });
}

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