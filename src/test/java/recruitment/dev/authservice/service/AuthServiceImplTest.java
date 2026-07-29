package recruitment.dev.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recruitment.dev.authservice.dto.LoginRequest;
import recruitment.dev.authservice.dto.LoginResponse;
import recruitment.dev.authservice.dto.LogoutRequest;
import recruitment.dev.authservice.dto.RefreshTokenRequest;
import recruitment.dev.authservice.dto.UpdateUserRoleRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private KeycloakUserAdminService keycloakUserAdminService;
    @Mock private UserRegistrationService userRegistrationService;
    @InjectMocks private AuthServiceImpl service;

    @Test
    void delegatesLoginRefreshAndLogoutToAuthenticationService() {
        LoginRequest login = new LoginRequest();
        LoginResponse expected = new LoginResponse();
        RefreshTokenRequest refresh = new RefreshTokenRequest();
        LogoutRequest logout = new LogoutRequest();
        when(authenticationService.login(login)).thenReturn(expected);
        when(authenticationService.refreshToken(refresh)).thenReturn(expected);

        assertThat(service.login(login)).isSameAs(expected);
        assertThat(service.refreshToken(refresh)).isSameAs(expected);
        service.logout(logout);

        verify(authenticationService).logout(logout);
    }

    @Test
    void delegatesRoleUpdateAndUserDeletionToKeycloakAdministration() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setOldRole("CANDIDATE");
        request.setNewRole("HR");

        service.updateUserRole("user-1", request);
        service.deleteUser("user-1");

        verify(keycloakUserAdminService).updateUserRole("user-1", "CANDIDATE", "HR");
        verify(keycloakUserAdminService).deleteUser("user-1");
    }
}
