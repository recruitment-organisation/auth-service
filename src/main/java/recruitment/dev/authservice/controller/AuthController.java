package recruitment.dev.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recruitment.dev.authservice.dto.*;

import recruitment.dev.authservice.feign.CreateEmployeeRequest;
import recruitment.dev.authservice.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody CreateUserRequest request) {

        return ResponseEntity.ok(authService.createUser(request));
    }
    @PreAuthorize("hasRole('HR')")
    @PostMapping("/create-employee")
    public ResponseEntity<CreateUserResponse> createEmployee(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(authService.createEmployee(request, "Bearer " + jwt.getTokenValue()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/companies/{companyId}/hr")
    public ResponseEntity<CreateHrResponse> createHr(@PathVariable Long companyId,
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateHrRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(authService.createHr(companyId, request, "Bearer " + jwt.getTokenValue()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/hr/{hrId}/status")
    public ResponseEntity<Void> setHrStatus(@PathVariable Long hrId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StatusRequest request) {
        authService.setHrStatus(hrId, request, "Bearer " + jwt.getTokenValue());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        authService.updateUserRole(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateMyAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMyAccountRequest request) {
        authService.updateMyAccount(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

}
