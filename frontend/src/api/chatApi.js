import { apiRequest } from "./apiClient";

export function fetchChatMessages(accessToken) {
  return apiRequest("/api/chat/messages", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function postChatMessage(accessToken, userId, content) {
  return apiRequest("/api/chat/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      id: userId,
      content,
    }),
  });
}
