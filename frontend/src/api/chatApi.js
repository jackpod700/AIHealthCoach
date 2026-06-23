import { ApiRequestError, apiRequest } from "./apiClient";

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

export async function postChatMessageStream(accessToken, content, handlers = {}) {
  const response = await fetch("/api/chat/messages/stream", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ content }),
  });

  if (!response.ok || !response.body) {
    const payload = await response.json().catch(() => null);
    throw new ApiRequestError(
      payload?.error?.message || "요청 처리 중 오류가 발생했습니다.",
      response.status,
      payload?.error?.code
    );
  }

  await readSseStream(response.body, handlers);
}

async function readSseStream(body, handlers = {}) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || "";

    events.forEach((rawEvent) => dispatchSseEvent(rawEvent, handlers));

    if (done) {
      break;
    }
  }

  if (buffer.trim()) {
    dispatchSseEvent(buffer, handlers);
  }
}

function dispatchSseEvent(rawEvent, handlers = {}) {
  const lines = rawEvent.split(/\r?\n/);
  const eventName = lines
    .find((line) => line.startsWith("event:"))
    ?.slice("event:".length)
    .trim();
  const data = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice("data:".length).trimStart())
    .join("\n");

  if (!eventName) {
    return;
  }

  const payload = data ? JSON.parse(data) : null;
  handlers[eventName]?.(payload);
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
