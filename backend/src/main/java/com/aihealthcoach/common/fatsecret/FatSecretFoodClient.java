package com.aihealthcoach.common.fatsecret;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretFoodItemResponse;
import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class FatSecretFoodClient {

    private final RestClient restClient;
    private final FatSecretProperties properties;

    public FatSecretFoodClient(RestClient.Builder restClientBuilder, FatSecretProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public FatSecretSearchTestResponse search(String accessToken, String query, int maxResults, String region, String language) {
        validateSearchRequest(accessToken, query);

        try {
            JsonNode response = restClient.get()
                    .uri(properties.foodSearchUrl(), uriBuilder -> buildSearchUri(
                            uriBuilder,
                            query,
                            maxResults,
                            region,
                            language
                    ))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(JsonNode.class);

            return toSearchResponse(response, query, maxResults);
        } catch (FatSecretException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw FatSecretException.searchRequestFailed(
                    FatSecretErrorParser.detailFromBody(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw FatSecretException.searchRequestFailed();
        }
    }

    private java.net.URI buildSearchUri(
            UriBuilder uriBuilder,
            String query,
            int maxResults,
            String region,
            String language
    ) {
        UriBuilder builder = uriBuilder
                .queryParam("search_expression", query)
                .queryParam("page_number", 0)
                .queryParam("max_results", maxResults)
                .queryParam("format", "json");

        if (!isBlank(region)) {
            builder.queryParam("region", region);
        }

        if (!isBlank(language)) {
            builder.queryParam("language", language);
        }

        return builder.build();
    }

    private FatSecretSearchTestResponse toSearchResponse(JsonNode root, String query, int maxResults) {
        String errorDetail = FatSecretErrorParser.detailFromJson(root);
        if (!isBlank(errorDetail)) {
            throw FatSecretException.responseInvalid(errorDetail);
        }

        JsonNode foodsSearch = searchResultNode(root);
        if (foodsSearch == null || foodsSearch.isMissingNode()) {
            throw FatSecretException.responseInvalid();
        }

        return new FatSecretSearchTestResponse(
                true,
                query,
                intOrNull(foodsSearch.path("total_results")),
                intOrNull(foodsSearch.path("page_number")),
                intOrDefault(foodsSearch.path("max_results"), maxResults),
                foodItems(foodsSearch)
        );
    }

    private JsonNode searchResultNode(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }

        JsonNode foodsSearch = root.path("foods_search");
        if (!foodsSearch.isMissingNode()) {
            return foodsSearch;
        }

        return root.path("foods");
    }

    private List<FatSecretFoodItemResponse> foodItems(JsonNode searchResultNode) {
        JsonNode foodNode = searchResultNode.path("results").path("food");
        if (foodNode.isMissingNode()) {
            foodNode = searchResultNode.path("food");
        }

        if (foodNode == null || foodNode.isMissingNode() || foodNode.isNull()) {
            return List.of();
        }

        List<FatSecretFoodItemResponse> items = new ArrayList<>();
        if (foodNode.isArray()) {
            foodNode.forEach(node -> items.add(foodItem(node)));
            return items;
        }

        items.add(foodItem(foodNode));
        return items;
    }

    private FatSecretFoodItemResponse foodItem(JsonNode node) {
        return new FatSecretFoodItemResponse(
                textOrNull(node.path("food_id")),
                textOrNull(node.path("food_name")),
                textOrNull(node.path("food_type")),
                textOrNull(node.path("brand_name")),
                textOrNull(node.path("food_url")),
                textOrNull(node.path("food_description"))
        );
    }

    private void validateSearchRequest(String accessToken, String query) {
        if (isBlank(properties.foodSearchUrl())) {
            throw FatSecretException.configMissing();
        }

        if (isBlank(accessToken) || isBlank(query)) {
            throw FatSecretException.responseInvalid();
        }
    }

    private Integer intOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.canConvertToInt()) {
            return node.asInt();
        }

        String text = node.asText();
        if (isBlank(text)) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw FatSecretException.responseInvalid();
        }
    }

    private Integer intOrDefault(JsonNode node, int defaultValue) {
        Integer value = intOrNull(node);
        return value == null ? defaultValue : value;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private boolean isBlank(String value) {
        return Objects.toString(value, "").isBlank();
    }
}
