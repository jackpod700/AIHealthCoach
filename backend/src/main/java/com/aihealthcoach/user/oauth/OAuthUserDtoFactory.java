package com.aihealthcoach.user.oauth;

import com.aihealthcoach.user.dto.OAuthUserDto;
import com.aihealthcoach.user.entity.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Locale;
import java.util.Map;

public class OAuthUserDtoFactory {

    private OAuthUserDtoFactory() {}

    public static OAuthUserDto from(String registrationId, OAuth2User oauth2User) {
        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());

        if(provider == OAuthProvider.GOOGLE) {
            return fromGoogle(oauth2User.getAttributes());
        }

        if(provider == OAuthProvider.NAVER) {
            return fromNaver(oauth2User.getAttributes());
        }
        throw new IllegalArgumentException("지원하지 않는 OAuth provider입니다: " + registrationId);
    }



    private static OAuthUserDto fromGoogle(Map<String, Object> attributes) {
        return new OAuthUserDto(
                OAuthProvider.GOOGLE,
                String.valueOf(attributes.get("sub")),
                StringOrNULL(attributes.get("email")),
                StringOrNULL(attributes.get("name"))
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuthUserDto fromNaver(Map<String, Object> attributes) {

        Map<String, Object> response = (Map<String, Object>)  attributes.get("response");

        return new OAuthUserDto(
                OAuthProvider.NAVER,
                String.valueOf(attributes.get("id")),
                StringOrNULL(attributes.get("email")),
                StringOrNULL(attributes.get("name"))
        );
    }

    private static String StringOrNULL(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
