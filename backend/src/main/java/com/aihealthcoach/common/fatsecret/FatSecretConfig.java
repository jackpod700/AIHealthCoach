package com.aihealthcoach.common.fatsecret;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FatSecretProperties.class)
public class FatSecretConfig {
}
