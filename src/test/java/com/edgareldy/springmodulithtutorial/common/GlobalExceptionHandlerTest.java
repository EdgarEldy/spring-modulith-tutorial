package com.edgareldy.springmodulithtutorial.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checks that {@link GlobalExceptionHandler} turns each kind of failure into the right status and
 * an {@link ApiResponse} error body.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
class GlobalExceptionHandlerTest {

    // A standalone MockMvc only registers the given controller and advice: no Spring context, no
    // security, no database. It isolates the handler's mapping from everything else.
    private final MockMvcTester mvc = MockMvcTester.create(MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build());

    @Test
    void _01_ShouldReturn404WithErrorEnvelope_WhenAResourceIsNotFound() {
        assertThat(mvc.get().uri("/test/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Widget with id 42 not found");
                    json.assertThat().extractingPath("$.data").isNull();
                    json.assertThat().extractingPath("$.timestamp").isNotNull();
                });
    }

    @Test
    void _02_ShouldReturn422_WhenABusinessRuleIsViolated() {
        assertThat(mvc.get().uri("/test/business-rule"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.message").isEqualTo("Rule broken");
    }

    @Test
    void _03_ShouldReturn400WithFieldErrors_WhenTheBodyFailsValidation() {
        assertThat(mvc.post().uri("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Validation failed");
                    json.assertThat().extractingPath("$.data.name").isEqualTo("must not be blank");
                });
    }

    @Test
    void _04_ShouldReturn400_WhenTheBodyIsNotValidJson() {
        assertThat(mvc.post().uri("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.message").isEqualTo("Malformed request body");
    }

    @Test
    void _05_ShouldReturn403_WhenAccessIsDenied() {
        assertThat(mvc.get().uri("/test/forbidden"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.message").isEqualTo("Access denied");
    }

    @Test
    void _06_ShouldKeepSpringMvcStatus_WhenTheHttpMethodIsNotSupported() {
        assertThat(mvc.delete().uri("/test/forbidden"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson().extractingPath("$.success").isEqualTo(false);
    }

    @Test
    void _07_ShouldReturn500WithoutInternals_WhenAnUnexpectedExceptionOccurs() {
        assertThat(mvc.get().uri("/test/unexpected"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson().extractingPath("$.message").isEqualTo("Unexpected error");
    }

    /**
     * Request body with one constrained field.
     * <p>
     * Created edgar.muhamyangabo on 9/26/26
     * Author : edgar.muhamyangabo
     * Date : 9/26/26
     * Project : spring-modulith-tutorial
     *
     * @param name a mandatory name
     */
    record NamedRequest(@NotBlank String name) {
    }

    /**
     * Test-only controller throwing one exception per endpoint.
     * <p>
     * Created edgar.muhamyangabo on 9/26/26
     * Author : edgar.muhamyangabo
     * Date : 9/26/26
     * Project : spring-modulith-tutorial
     */
    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw ResourceNotFoundException.of("Widget", 42);
        }

        @GetMapping("/test/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Rule broken");
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody NamedRequest request) {
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("nope");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
