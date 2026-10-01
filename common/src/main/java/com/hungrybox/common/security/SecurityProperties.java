package com.hungrybox.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security settings shared by every service.
 *
 * <pre>
 * hungrybox.security.jwt-secret       HMAC secret, at least 32 bytes
 * hungrybox.security.jwt-ttl-minutes  token lifetime (default 120)
 * hungrybox.security.public-paths     "POST /api/users/login", "/actuator/**", ...
 * </pre>
 */
@ConfigurationProperties(prefix = "hungrybox.security")
public record SecurityProperties(String jwtSecret, Long jwtTtlMinutes, List<String> publicPaths) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("hungrybox.security.jwt-secret must be configured");
        }
        if (jwtTtlMinutes == null) {
            jwtTtlMinutes = 120L;
        }
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
