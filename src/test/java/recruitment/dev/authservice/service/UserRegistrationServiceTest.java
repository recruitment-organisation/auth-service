package recruitment.dev.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recruitment.dev.authservice.client.CandidateClient;
import recruitment.dev.authservice.client.EmployeeClient;
import recruitment.dev.authservice.dto.CreateUserRequest;
import recruitment.dev.authservice.exception.AuthException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock private KeycloakUserAdminService keycloakUserAdminService;
    @Mock private CandidateClient candidateClient;
    @Mock private EmployeeClient employeeClient;
    @InjectMocks private UserRegistrationService service;

    @Test
    void createsKeycloakAccountAssignsCandidateRoleAndProfile() {
        CreateUserRequest request = request();
        UserRepresentation user = new UserRepresentation();
        when(keycloakUserAdminService.userExists("ada@test.local")).thenReturn(false);
        when(keycloakUserAdminService.generateUsername("Ada", "Lovelace")).thenReturn("AdaLovelace");
        when(keycloakUserAdminService.buildUserRepresentation(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(user);
        when(keycloakUserAdminService.createKeycloakUser(user)).thenReturn("keycloak-1");

        assertThat(service.createUser(request)).isEqualTo("Utilisateur créé avec succès ! id=keycloak-1");

        verify(keycloakUserAdminService).assignRole("keycloak-1", "ROLE_CANDIDATE");
        ArgumentCaptor<recruitment.dev.authservice.feign.CandidateDto> candidate = ArgumentCaptor.forClass(recruitment.dev.authservice.feign.CandidateDto.class);
        verify(candidateClient).createCandidate(candidate.capture());
        assertThat(candidate.getValue().getKeycloakId()).isEqualTo("keycloak-1");
        assertThat(candidate.getValue().getEmail()).isEqualTo("ada@test.local");
        verifyNoInteractions(employeeClient);
    }

    @Test
    void rejectsExistingEmailBeforeCreatingKeycloakUser() {
        when(keycloakUserAdminService.userExists("ada@test.local")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Email already exists");
        verify(keycloakUserAdminService, never()).createKeycloakUser(any());
    }

    @Test
    void rollsBackKeycloakAccountWhenCandidateServiceFails() {
        CreateUserRequest request = request();
        when(keycloakUserAdminService.userExists("ada@test.local")).thenReturn(false);
        when(keycloakUserAdminService.generateUsername("Ada", "Lovelace")).thenReturn("AdaLovelace");
        when(keycloakUserAdminService.buildUserRepresentation(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(new UserRepresentation());
        when(keycloakUserAdminService.createKeycloakUser(any())).thenReturn("keycloak-2");
        doThrow(new RuntimeException("candidate-service unavailable")).when(candidateClient).createCandidate(any());

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Rolled back");
        verify(keycloakUserAdminService).rollbackKeycloakUser("keycloak-2");
    }

    private CreateUserRequest request() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("ada@test.local"); request.setPassword("StrongPassword1!");
        request.setFirstName("Ada"); request.setLastName("Lovelace"); request.setPhone("22000000");
        request.setLocation("Tunis"); request.setAvailable(true);
        return request;
    }
}
