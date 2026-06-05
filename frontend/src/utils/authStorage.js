const USER_ID_KEY = "ai-health-user-id";
const ACCESS_TOKEN_KEY = "ai-health-access-token";
const USER_KEY = "ai-health-user";
const PROFILE_KEY = "ai-health-profile";

export function loadAuthSession() {
  return {
    userId: Number(localStorage.getItem(USER_ID_KEY)) || null,
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY) || "",
    user: JSON.parse(localStorage.getItem(USER_KEY) || "null"),
  };
}

export function saveAuthSession({ userId, accessToken, user }) {
  localStorage.setItem(USER_ID_KEY, String(userId));
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuthSession() {
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

export function loadStoredProfile() {
  return JSON.parse(localStorage.getItem(PROFILE_KEY) || "null");
}

export function saveStoredProfile(profile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
}

export function clearStoredProfile() {
  localStorage.removeItem(PROFILE_KEY);
}
