package com.edgareldy.springmodulithtutorial.auth;

import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers an authenticated caller refused by the filter chain with a 403 {@link ApiResponse} error envelope.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// Counterpart of the entry point for "known caller, not allowed". A failed @PreAuthorize inside a
// controller is still rendered by the GlobalExceptionHandler; this handler covers refusals raised by
// the filters themselves, so both paths produce the same body.
@Component
class ApiResponseAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    /**
     * @param jsonMapper the JSON mapper configured by Spring Boot
     */
    ApiResponseAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error("Access denied"));
    }
}
