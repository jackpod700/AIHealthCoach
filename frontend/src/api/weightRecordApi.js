import { apiRequest } from "./apiClient";

export function fetchWeightRecords(accessToken, range) {
  const searchParams = new URLSearchParams();

  if (range?.from && range?.to) {
    searchParams.set("from", range.from);
    searchParams.set("to", range.to);
  }

  const query = searchParams.toString();

  return apiRequest(`/api/weight-records${query ? `?${query}` : ""}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export function saveWeightRecord(accessToken, record) {
  return apiRequest("/api/weight-records", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(record),
  });
}

export function deleteWeightRecord(accessToken, recordDate) {
  return apiRequest("/api/weight-records", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ recordDate }),
  });
}
