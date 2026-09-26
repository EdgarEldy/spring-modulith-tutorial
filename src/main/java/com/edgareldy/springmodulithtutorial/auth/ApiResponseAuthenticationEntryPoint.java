package com.edgareldy.springmodulithtutorial.auth;

import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers an unauthenticated request (no token, or an invalid, expired or revoked one) with a 401
 * carrying the same {@link ApiResponse} error envelope as every other failure of the API.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// An AuthenticationEntryPoint runs inside the security filter chain, before any controller: the
// GlobalExceptionHandler never sees these failures. Writing the envelope here keeps the error shape
// identical whether a request is refused by the filters or by a controller.
@Component
class ApiResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    /**
     * @param jsonMapper the JSON mapper configured by Spring Boot, the same one Spring MVC uses
     */
    ApiResponseAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.error("Authentication required"));
    }
}
