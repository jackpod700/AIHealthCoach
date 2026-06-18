import { apiRequest } from "./apiClient";

export function signupUser(credentials) {
  return apiRequest("/api/user/signup", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentials),
  });
}

export function loginUser(credentials) {
  return apiRequest("/api/user/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentials),
  });
}

export function refreshAccessToken() {

  return apiRequest("/api/user/token/refresh", {

    method: "POST",

    credentials: "include",

  });

}

export function getOAuthLoginUrl(provider) {

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  return `${apiBaseUrl}/api/oauth/login/${provider}`;

}