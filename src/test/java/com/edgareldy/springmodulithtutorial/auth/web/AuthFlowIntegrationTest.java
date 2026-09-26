package com.edgareldy.springmodulithtutorial.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmodulithtutorial.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * End-to-end HTTP tests of the auth endpoints: registration, activation from the logged link, login,
 * profile, logout, forgot-password enumeration protection and the password reset flow.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// OutputCaptureExtension records what the application writes to the console: the activation link and
// the reset token are logged in place of an email, so the test reads them exactly as a developer would.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void _01_ShouldRejectTheTokenAfterLogout_WhenFollowingTheWholeAccountLifecycle(CapturedOutput output) {
        String email = uniqueEmail();

        assertThat(register(email))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(true);
                    json.assertThat().extractingPath("$.data.email").isEqualTo(email);
                    json.assertThat().extractingPath("$.data.roles[0]").isEqualTo("USER");
                    json.assertThat().doesNotHavePath("$.data.password");
                });
        assertThat(login(email, PASSWORD))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.message").isEqualTo("Account is not activated");

        String activationToken = captured(output, "Activation link for " + email + ": \\S+token=(\\S+)");
        assertThat(mvc.get().uri("/api/v1/auth/activate-account").param("token", activationToken))
                .hasStatusOk()
                .bodyJson().extractingPath("$.message").isEqualTo("Account activated");
        assertThat(mvc.get().uri("/api/v1/auth/activate-account").param("token", activationToken))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        String jwt = accessToken(email, PASSWORD);
        assertThat(mvc.get().uri("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.email").isEqualTo(email);

        assertThat(mvc.post().uri("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .hasStatusOk()
                .bodyJson().extractingPath("$.message").isEqualTo("Logged out");
        assertThat(mvc.get().uri("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.success").isEqualTo(false);
                    json.assertThat().extractingPath("$.message").isEqualTo("Authentication required");
                });
    }

    @Test
    void _02_ShouldAnswerIdentically_WhenForgotPasswordIsCalledForAKnownAndAnUnknownEmail(CapturedOutput output) {
        String known = activatedAccount(output);

        MvcTestResult forKnown = forgotPassword(known);
        MvcTestResult forUnknown = forgotPassword(uniqueEmail());

        assertThat(forKnown).hasStatusOk();
        assertThat(forUnknown).hasStatus(forKnown.getResponse().getStatus());
        assertThat(withoutTimestamp(body(forUnknown))).isEqualTo(withoutTimestamp(body(forKnown)));
    }

    @Test
    void _03_ShouldRefuseTheSecondUse_WhenAResetTokenHasAlreadyChangedThePassword(CapturedOutput output) {
        String email = activatedAccount(output);
        forgotPassword(email);
        String resetToken = captured(output, "Password reset token for " + email + ": (\\S+)");
        String reset = "{\"token\":\"" + resetToken + "\",\"newPassword\":\"brand-new-password\"}";

        assertThat(mvc.post().uri("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(reset))
                .hasStatusOk();
        assertThat(login(email, PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, "brand-new-password")).hasStatusOk();
        assertThat(mvc.post().uri("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(reset))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.message").isEqualTo("Invalid or expired password reset token");
    }

    @Test
    void _04_ShouldAnswer401WithAnErrorEnvelope_WhenMeIsCalledWithoutAToken() {
        assertThat(mvc.get().uri("/api/v1/auth/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.message").isEqualTo("Authentication required");
    }

    @Test
    void _05_ShouldAnswer422_WhenTheEmailIsAlreadyRegistered() {
        String email = uniqueEmail();
        assertThat(register(email)).hasStatus(HttpStatus.CREATED);

        assertThat(register(email)).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void _06_ShouldAnswer400WithTheInvalidFields_WhenTheRegistrationIsInvalid() {
        String body = "{\"firstName\":\"\",\"lastName\":\"Lovelace\",\"email\":\"not-an-email\",\"password\":\"short\"}";

        assertThat(mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().hasPath("$.data.firstName");
                    json.assertThat().hasPath("$.data.email");
                    json.assertThat().hasPath("$.data.password");
                });
    }

    private String activatedAccount(CapturedOutput output) {
        String email = uniqueEmail();
        assertThat(register(email)).hasStatus(HttpStatus.CREATED);
        String token = captured(output, "Activation link for " + email + ": \\S+token=(\\S+)");
        assertThat(mvc.get().uri("/api/v1/auth/activate-account").param("token", token)).hasStatusOk();
        return email;
    }

    private MvcTestResult register(String email) {
        String body = "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"" + email
                + "\",\"password\":\"" + PASSWORD + "\"}";
        return mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult login(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private String accessToken(String email, String password) {
        MvcTestResult result = login(email, password);
        assertThat(result).hasStatusOk();
        return JsonPath.read(body(result), "$.data.accessToken");
    }

    private MvcTestResult forgotPassword(String email) {
        return mvc.post().uri("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}").exchange();
    }

    private static String captured(CapturedOutput output, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(output.getOut());
        assertThat(matcher.find()).as("log line matching %s", regex).isTrue();
        return matcher.group(1);
    }

    private static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
