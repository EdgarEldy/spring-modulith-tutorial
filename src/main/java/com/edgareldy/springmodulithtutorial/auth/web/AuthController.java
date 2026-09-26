package com.edgareldy.springmodulithtutorial.auth.web;

import com.edgareldy.springmodulithtutorial.auth.LoginResponse;
import com.edgareldy.springmodulithtutorial.auth.UserProfile;
import com.edgareldy.springmodulithtutorial.auth.UserService;
import com.edgareldy.springmodulithtutorial.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point of the auth module: validates the request, delegates to {@link UserService} and
 * wraps the result in an {@link ApiResponse}. Which routes are public is decided by the security chain.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final UserService userService;

    /**
     * @param userService the account lifecycle
     */
    AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * @param request the new account
     * @return the created account, disabled until its activation link is followed
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        UserProfile profile = userService.register(
                request.firstName(), request.lastName(), request.email(), request.password());
        return ApiResponse.success(profile, "Account created, check the activation link");
    }

    /**
     * @param token the value from the activation link
     * @return an empty success
     */
    @GetMapping("/activate-account")
    ApiResponse<Void> activate(@RequestParam String token) {
        userService.activate(token);
        return ApiResponse.success(null, "Account activated");
    }

    /**
     * @param request the credentials
     * @return the issued JWT
     */
    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request.email(), request.password()), "Login successful");
    }

    /**
     * @param jwt the token of the current request, revoked by this call
     * @return an empty success
     */
    // @AuthenticationPrincipal hands over the principal the resource server stored in the
    // SecurityContext, here the decoded Jwt: no need to parse the Authorization header again.
    @PostMapping("/logout")
    ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        userService.logout(jwt);
        return ApiResponse.success(null, "Logged out");
    }

    /**
     * @param jwt the token of the current request, whose subject is the account id
     * @return the profile of the authenticated account
     */
    @GetMapping("/me")
    ApiResponse<UserProfile> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.me(Long.valueOf(jwt.getSubject())), "Current user profile");
    }

    /**
     * @param request the email of the account
     * @return the same success whether or not the account exists
     */
    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.email());
        return ApiResponse.success(null, "If an account exists for this email, a password reset link has been sent");
    }

    /**
     * @param request the reset token and the new password
     * @return an empty success
     */
    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success(null, "Password has been reset");
    }
}
