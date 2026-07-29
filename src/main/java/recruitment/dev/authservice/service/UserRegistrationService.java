package recruitment.dev.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import recruitment.dev.authservice.client.CandidateClient;
import recruitment.dev.authservice.client.EmployeeClient;
import recruitment.dev.authservice.dto.CreateUserRequest;
import recruitment.dev.authservice.dto.CreateUserResponse;
import recruitment.dev.authservice.exception.AuthException;
import recruitment.dev.authservice.feign.CandidateDto;
import recruitment.dev.authservice.feign.CreateEmployeeRequest;
import recruitment.dev.authservice.feign.EmployeeDto;

import java.util.function.Consumer;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final KeycloakUserAdminService keycloakUserAdminService;
    private final CandidateClient candidateClient;
    private final EmployeeClient employeeClient;

    public String createUser(CreateUserRequest request) {
        CreateUserResponse created = createAccount(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                keycloakUserId ->{

                    keycloakUserAdminService.assignRole(
                            keycloakUserId,
                            "ROLE_CANDIDATE"
                    );
                        candidateClient.createCandidate(CandidateDto.builder()
                        .keycloakId(keycloakUserId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .location(request.getLocation())
                        .available(request.getAvailable())
                        .linkedinUrl(request.getLinkedinUrl())
                        .githubUrl(request.getGithubUrl())
                        .build());
                },
                "User created but candidate profile creation failed. Rolled back."
        );
        return "Utilisateur créé avec succès ! id=" + created.getKeycloakUserId();
    }

    public CreateUserResponse createEmployee(CreateEmployeeRequest request) {
        return createAccount(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                keycloakUserId -> {
                    keycloakUserAdminService.assignRole(
                            keycloakUserId,
                            "ROLE_EMPLOYEE"
                    );
                    employeeClient.createEmployee(EmployeeDto.builder()
                            .keycloakId(keycloakUserId)
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .hireDate(request.getHireDate())
                            .position(request.getPosition())
                            .roleId(request.getRoleId())
                            .departmentId(request.getDepartmentId())
                            .build());
                },
                "Employee profile creation failed. Keycloak user rolled back."
        );
    }

    private CreateUserResponse createAccount(
            String email,
            String firstName,
            String lastName,
            String password,
            Consumer<String> profileCreator,
            String failureMessage) {

        if (keycloakUserAdminService.userExists(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already exists.");
        }

        String username = keycloakUserAdminService.generateUsername(firstName, lastName);
        UserRepresentation user = keycloakUserAdminService.buildUserRepresentation(
                email, firstName, lastName, password, username);
        String keycloakUserId = keycloakUserAdminService.createKeycloakUser(user);

        try {
            profileCreator.accept(keycloakUserId);
        } catch (Exception ex) {
            log.error("Profile creation failed for keycloak user {}, rolling back", keycloakUserId, ex);
            keycloakUserAdminService.rollbackKeycloakUser(keycloakUserId);
            throw new AuthException(HttpStatus.BAD_GATEWAY, failureMessage, ex);
        }

        return CreateUserResponse.builder()
                .keycloakUserId(keycloakUserId)
                .username(username)
                .email(email)
                .build();
    }
}