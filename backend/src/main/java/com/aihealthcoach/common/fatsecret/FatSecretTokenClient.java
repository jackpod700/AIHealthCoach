package com.aihealthcoach.common.fatsecret;

import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretTokenResponse;

@Component
public class FatSecretTokenClient {

    private final RestClient restClient;
    private final FatSecretProperties properties;

    public FatSecretTokenClient(RestClient.Builder restClientBuilder, FatSecretProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public String requestAccessToken() {
        validateCredentials();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", blankToDefault(properties.scope(), "basic"));

        try {
            FatSecretTokenResponse tokenResponse = restClient.post()
                    .uri(properties.tokenUrl())
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(FatSecretTokenResponse.class);

            if (tokenResponse == null || isBlank(tokenResponse.access_token())) {
                throw FatSecretException.responseInvalid();
            }

            return tokenResponse.access_token();
        } catch (FatSecretException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw FatSecretException.tokenRequestFailed(
                    FatSecretErrorParser.detailFromBody(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw FatSecretException.tokenRequestFailed();
        }
    }

    private void validateCredentials() {
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret()) || isBlank(properties.tokenUrl())) {
            throw FatSecretException.configMissing();
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return Objects.toString(value, "").isBlank();
    }
}
