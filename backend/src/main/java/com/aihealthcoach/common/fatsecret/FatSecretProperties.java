package com.aihealthcoach.common.fatsecret;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fatsecret")
public record FatSecretProperties(
        String clientId,
        String clientSecret,
        String tokenUrl,
        String foodSearchUrl,
        String scope,
        String region,
        String language
) {
}
