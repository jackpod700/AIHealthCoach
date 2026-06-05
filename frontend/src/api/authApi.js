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
