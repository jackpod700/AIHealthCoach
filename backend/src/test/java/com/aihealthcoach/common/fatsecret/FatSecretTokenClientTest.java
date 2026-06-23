package com.aihealthcoach.common.fatsecret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class FatSecretTokenClientTest {

    @Test
    void requestAccessTokenUsesClientCredentialsGrant() {
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
        FatSecretTokenClient client = new FatSecretTokenClient(builder, properties);
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString("client-id:client-secret".getBytes(StandardCharsets.UTF_8));

        server.expect(once(), requestTo(properties.tokenUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andExpect(content().string(containsString("scope=basic")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "token_type": "Bearer",
                          "expires_in": 86400
                        }
                        """, MediaType.APPLICATION_JSON));

        String accessToken = client.requestAccessToken();

        assertThat(accessToken).isEqualTo("access-token");
        server.verify();
    }

    @Test
    void requestAccessTokenRejectsMissingCredentials() {
        FatSecretProperties properties = new FatSecretProperties(
                "",
                "",
                "https://oauth.fatsecret.com/connect/token",
                "https://platform.fatsecret.com/rest/foods/search/v5",
                "basic",
                "KR",
                "ko"
        );
        FatSecretTokenClient client = new FatSecretTokenClient(RestClient.builder(), properties);

        assertThatThrownBy(client::requestAccessToken)
                .isInstanceOf(FatSecretException.class)
                .extracting("errorCode")
                .isEqualTo(FatSecretErrorCode.FATSECRET_CONFIG_MISSING);
    }

    @Test
    void requestAccessTokenIncludesFatSecretOAuthErrorDetail() {
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
        FatSecretTokenClient client = new FatSecretTokenClient(builder, properties);

        server.expect(once(), requestTo(properties.tokenUrl()))
                .andRespond(withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_client",
                                  "error_description": "Client authentication failed"
                                }
                                """));

        assertThatThrownBy(client::requestAccessToken)
                .isInstanceOf(FatSecretException.class)
                .hasMessageContaining("FatSecret error code=invalid_client")
                .hasMessageContaining("Client authentication failed")
                .hasMessageNotContaining("client-secret");

        server.verify();
    }
}
