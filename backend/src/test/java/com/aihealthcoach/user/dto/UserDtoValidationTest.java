package com.aihealthcoach.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UserDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void userProfileUpdateAllowsWeightWithoutTwoDecimalPlaces() {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .heightCm(new BigDecimal("172.5"))
                .currentWeightKg(new BigDecimal("68.456"))
                .targetWeightKg(new BigDecimal("65"))
                .goalType("WEIGHT_LOSS")
                .gender("FEMALE")
                .age(27)
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
