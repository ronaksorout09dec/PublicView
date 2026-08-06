package com.skyheights.realestate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.isBlank()) return false;
        String clean = phone.replaceAll("[^0-9]", "");
        return clean.matches("^[6-9]\\d{9}$");
    }
}
