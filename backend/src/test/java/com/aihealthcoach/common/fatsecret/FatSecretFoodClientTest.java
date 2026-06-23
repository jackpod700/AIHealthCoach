package com.aihealthcoach.common.fatsecret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;

class FatSecretFoodClientTest {

    @Test
    void searchUsesBearerTokenAndSearchParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FatSecretProperties properties = new FatSecretProperties(
                "client-id",
                "client-secret",
                "https://oauth.fatsecret.com/connect/token",
                "https://platform.fatsecret.com/rest/foods/search/v5",
                "basic",
                "KR",
                "ko"
        );
        FatSecretFoodClient client = new FatSecretFoodClient(builder, properties);

        server.expect(once(), requestTo(containsString(properties.foodSearchUrl())))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(requestTo(containsString("search_expression=apple")))
                .andExpect(requestTo(containsString("max_results=5")))
                .andExpect(requestTo(containsString("page_number=0")))
                .andExpect(requestTo(containsString("region=KR")))
                .andExpect(requestTo(containsString("language=ko")))
                .andExpect(requestTo(containsString("format=json")))
                .andRespond(withSuccess("""
                        {
                          "foods_search": {
                            "max_results": "5",
                            "total_results": "1",
                            "page_number": "0",
                            "results": {
                              "food": [
                                {
                                  "food_id": "123",
                                  "food_name": "Apple",
                                  "food_type": "Generic",
                                  "food_url": "https://example.test/apple",
                                  "food_description": "Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"
                                }
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        FatSecretSearchTestResponse response = client.search("access-token", "apple", 5, "KR", "ko");

        assertThat(response.connected()).isTrue();
        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).foodId()).isEqualTo("123");
        assertThat(response.items().get(0).foodName()).isEqualTo("Apple");
        assertThat(response.items().get(0).foodDescription()).contains("Calories");
        server.verify();
    }

    @Test
    void searchIncludesFatSecretErrorDetailWhenBodyContainsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FatSecretProperties properties = new FatSecretProperties(
                "client-id",
                "client-secret",
                "https://oauth.fatsecret.com/connect/token",
                "https://platform.fatsecret.com/rest/foods/search/v5",
                "basic",
                "KR",
                "ko"
        );
        FatSecretFoodClient client = new FatSecretFoodClient(builder, properties);

        server.expect(once(), requestTo(containsString(properties.foodSearchUrl())))
                .andRespond(withSuccess("""
                        {
                          "error": {
                            "code": "21",
                            "message": "Premiere access required"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("access-token", "apple", 5, "KR", "ko"))
                .isInstanceOf(FatSecretException.class)
                .hasMessageContaining("FatSecret error code=21")
                .hasMessageContaining("Premiere access required")
                .hasMessageNotContaining("access-token");

        server.verify();
    }

    @Test
    void searchParsesV1FoodsResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FatSecretProperties properties = new FatSecretProperties(
                "client-id",
                "client-secret",
                "https://oauth.fatsecret.com/connect/token",
                "https://platform.fatsecret.com/rest/foods/search/v1",
                "basic",
                "KR",
                "ko"
        );
        FatSecretFoodClient client = new FatSecretFoodClient(builder, properties);

        server.expect(once(), requestTo(containsString(properties.foodSearchUrl())))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "foods": {
                            "food": {
                              "food_id": "41963",
                              "food_name": "Cheeseburger",
                              "food_type": "Brand",
                              "brand_name": "McDonald's",
                              "food_url": "https://foods.fatsecret.com/calories-nutrition/mcdonalds/cheeseburger",
                              "food_description": "Per 1 serving - Calories: 300kcal | Fat: 13.00g | Carbs: 32.00g | Protein: 15.00g"
                            },
                            "max_results": "1",
                            "page_number": "0",
                            "total_results": "220"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        FatSecretSearchTestResponse response = client.search("access-token", "apple", 5, "KR", "ko");

        assertThat(response.connected()).isTrue();
        assertThat(response.totalResults()).isEqualTo(220);
        assertThat(response.maxResults()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).foodId()).isEqualTo("41963");
        assertThat(response.items().get(0).brandName()).isEqualTo("McDonald's");
        assertThat(response.items().get(0).foodDescription()).contains("Calories: 300kcal");
        server.verify();
    }
}
