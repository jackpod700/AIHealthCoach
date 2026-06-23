package com.aihealthcoach.common.fatsecret;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class FatSecretErrorParser {

    private static final int MAX_DETAIL_LENGTH = 500;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FatSecretErrorParser() {
    }

    static String detailFromBody(String body) {
        if (isBlank(body)) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String parsed = detailFromJson(root);
            if (!isBlank(parsed)) {
                return parsed;
            }
        } catch (Exception ignored) {
            return "FatSecret error body=" + sanitize(body);
        }

        return "FatSecret error body=" + sanitize(body);
    }

    static String detailFromJson(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }

        String error = textOrNull(root.path("error"));
        String description = firstText(root.path("error_description"), root.path("message"));
        if (!isBlank(error) || !isBlank(description)) {
            return format(error, description);
        }

        String detail = detailFromErrorNode(root.path("error"));
        if (!isBlank(detail)) {
            return detail;
        }

        detail = detailFromErrorNode(root.path("errors").path("error"));
        if (!isBlank(detail)) {
            return detail;
        }

        return null;
    }

    private static String detailFromErrorNode(JsonNode errorNode) {
        if (errorNode == null || errorNode.isMissingNode() || errorNode.isNull()) {
            return null;
        }

        if (errorNode.isTextual()) {
            return format(errorNode.asText(), null);
        }

        String code = firstText(errorNode.path("code"), errorNode.path("error_code"), errorNode.path("id"));
        String message = firstText(errorNode.path("message"), errorNode.path("error_description"), errorNode.path("description"));

        if (isBlank(code) && isBlank(message)) {
            return null;
        }

        return format(code, message);
    }

    private static String format(String code, String message) {
        if (!isBlank(code) && !isBlank(message)) {
            return "FatSecret error code=" + sanitize(code) + ", message=" + sanitize(message);
        }

        if (!isBlank(code)) {
            return "FatSecret error code=" + sanitize(code);
        }

        return "FatSecret error message=" + sanitize(message);
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String text = textOrNull(node);
            if (!isBlank(text)) {
                return text;
            }
        }

        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.isObject() || node.isArray()) {
            return null;
        }

        return node.asText();
    }

    private static String sanitize(String value) {
        String sanitized = Objects.toString(value, "")
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [REDACTED]")
                .replaceAll("(?i)basic\\s+[A-Za-z0-9._~+/=-]+", "Basic [REDACTED]")
                .replaceAll("(?i)(access_token|client_secret|authorization)\"?\\s*[:=]\\s*\"?[^\"]+", "$1=[REDACTED]");

        if (sanitized.length() <= MAX_DETAIL_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(0, MAX_DETAIL_LENGTH) + "...";
    }

    private static boolean isBlank(String value) {
        return Objects.toString(value, "").isBlank();
    }
}
