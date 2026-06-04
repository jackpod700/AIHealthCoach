export class ApiRequestError extends Error {
  constructor(message, status, code) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = code;
  }
}

export async function apiRequest(url, options = {}) {
  const response = await fetch(url, options);

  if (response.status === 204) {
    return null;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    throw new ApiRequestError(
      payload?.error?.message || "요청 처리 중 오류가 발생했습니다.",
      response.status,
      payload?.error?.code
    );
  }

  return payload?.data ?? payload;
}

export function isAuthenticationError(error) {
  return (
    error?.status === 401 ||
    error?.status === 403 ||
    error?.code === "UNAUTHORIZED" ||
    error?.code === "FORBIDDEN" ||
    error?.message?.includes("JWT") ||
    error?.message?.includes("토큰") ||
    error?.message?.includes("인증")
  );
}
