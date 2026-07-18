package recruitment.dev.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import recruitment.dev.authservice.dto.*;
import recruitment.dev.authservice.feign.CreateEmployeeRequest;
import recruitment.dev.authservice.service.AuthService;
import recruitment.dev.authservice.service.AuthenticationService;

@Service
@Validated
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationService authenticationService;
    private final KeycloakUserAdminService keycloakUserAdminService;
    private final UserRegistrationService userRegistrationService;

    @Override
    public LoginResponse login(LoginRequest request) {
        return authenticationService.login(request);
    }

    @Override
    public void logout(LogoutRequest request) {
        authenticationService.logout(request);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        return authenticationService.refreshToken(request);
    }

    @Override
    public CreateUserResponse createEmployee(CreateEmployeeRequest request) {
        return userRegistrationService.createEmployee(request);
    }

    @Override
    public String createUser(CreateUserRequest request) {
        return userRegistrationService.createUser(request);
    }

    @Override
    public void deleteUser(String userId) {
        keycloakUserAdminService.deleteUser(userId);
    }

}