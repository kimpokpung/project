package home.project.service;

import home.project.domain.CustomOptionalResponseBody;
import home.project.domain.CustomOptionalResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ValidationCheck {
    public CustomOptionalResponseEntity<?> validationChecks(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> responseMap = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                responseMap.put(error.getField(), error.getDefaultMessage());
            }
            CustomOptionalResponseBody<Optional<Map<String, String>>> errorBody = new CustomOptionalResponseBody<>(Optional.of(responseMap), "Validation failed", HttpStatus.BAD_REQUEST.value());
            return new CustomOptionalResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        }
        return null;
    }
}
