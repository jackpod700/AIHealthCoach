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

export function postChatImageMessage(accessToken, content, images = []) {
  const formData = new FormData();

  if (content?.trim()) {
    formData.append("content", content.trim());
  }

  images.forEach((image) => {
    formData.append("images", image);
  });

  return apiRequest("/api/chat/messages/images", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: formData,
  });
}

export function confirmMealProposal(accessToken, proposal) {
  return apiRequest("/api/chat/meal-proposals/confirm", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(proposal),
  });
}
