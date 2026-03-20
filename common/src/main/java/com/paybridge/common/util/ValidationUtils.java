package com.paybridge.common.util;


import com.paybridge.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility for bean validation (JSR-380).
 *
 * 📚 EDUCATION:
 * - Used to manually validate DTOs before processing
 * - Converts ConstraintViolation to Map<String, String> for ErrorDetail
 * - Stateless → can be used as static utility
 */
public class ValidationUtils {

	private static final Validator VALIDATOR;

	static {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			VALIDATOR = factory.getValidator();
		}
	}

	/**
	 * Validate a bean and return field errors
	 * @return Map of field name → error message, or empty map if valid
	 */
	public static <T> Map<String, String> validate(T bean) {
		Set<ConstraintViolation<T>> violations = VALIDATOR.validate(bean);
		Map<String, String> errors = new HashMap<>();

		for (ConstraintViolation<T> violation : violations) {
			String field = violation.getPropertyPath().toString();
			String message = violation.getMessage();
			errors.put(field, message);
		}

		return errors;
	}

	/**
	 * Validate and throw ValidationException if invalid
	 * @throws ValidationException if validation fails
	 */
	public static <T> void validateOrThrow(T bean) throws ValidationException {
		Map<String, String> errors = validate(bean);
		if (!errors.isEmpty()) {
			throw new ValidationException(errors);
		}
	}
}
