package com.aihealthcoach.user.oauth;

import com.aihealthcoach.user.dto.OAuthUserDto;
import com.aihealthcoach.user.entity.OAuthProvider;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class OAuthUserDtoFactory {

    private OAuthUserDtoFactory() {
    }

    public static OAuthUserDto from(String registrationId, OAuth2User oauth2User) {
        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());

        if (provider == OAuthProvider.GOOGLE) {
            return fromGoogle(oauth2User.getAttributes());
        }

        if (provider == OAuthProvider.NAVER) {
            return fromNaver(oauth2User.getAttributes());
        }

        throw new IllegalArgumentException("지원하지 않는 OAuth provider입니다: " + registrationId);
    }

    private static OAuthUserDto fromGoogle(Map<String, Object> attributes) {
        return new OAuthUserDto(
                OAuthProvider.GOOGLE,
                stringOrNull(attributes.get("sub")),
                stringOrNull(attributes.get("email")),
                stringOrNull(attributes.get("name"))
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuthUserDto fromNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) {
            throw new IllegalArgumentException("네이버 OAuth 응답에 response 값이 없습니다.");
        }

        return new OAuthUserDto(
                OAuthProvider.NAVER,
                stringOrNull(response.get("id")),
                stringOrNull(response.get("email")),
                stringOrNull(response.get("name"))
        );
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}