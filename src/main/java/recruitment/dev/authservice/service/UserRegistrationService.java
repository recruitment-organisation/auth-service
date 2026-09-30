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
import java.security.SecureRandom;
import recruitment.dev.authservice.dto.CreateHrRequest;
import recruitment.dev.authservice.dto.CreateHrResponse;
import recruitment.dev.authservice.feign.CreateHrProfileRequest;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();

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
                            "CANDIDATE"
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

    public CreateUserResponse createEmployee(CreateEmployeeRequest request, String authorization) {
        return createAccount(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                keycloakUserId -> {
                    String keycloakRole = resolveKeycloakRole(authorization, request.getRoleId());
                    keycloakUserAdminService.assignRole(
                            keycloakUserId,
                            keycloakRole
                    );
                    employeeClient.createEmployee(authorization, EmployeeDto.builder()
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

    public CreateHrResponse createHr(Long companyId, CreateHrRequest request, String authorization) {
        if (keycloakUserAdminService.userExists(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "Email already exists.");
        }
        String temporaryPassword = temporaryPassword();
        UserRepresentation user = keycloakUserAdminService.buildManagedHr(
                request.email().trim(), request.firstName(), request.lastName(), temporaryPassword,
                companyId, request.active() == null || request.active());
        String keycloakId = keycloakUserAdminService.createKeycloakUser(user);
        try {
            keycloakUserAdminService.assignRole(keycloakId, "HR");
            EmployeeDto employee = employeeClient.createHrProfile(authorization, companyId,
                    new CreateHrProfileRequest(keycloakId, request.firstName(), request.lastName(), request.email(), request.phone(), request.active()));
            return new CreateHrResponse(keycloakId, employee.getId(), request.email(), temporaryPassword, companyId);
        } catch (RuntimeException exception) {
            keycloakUserAdminService.rollbackKeycloakUser(keycloakId);
            throw new AuthException(HttpStatus.BAD_GATEWAY, "HR profile creation failed. Keycloak user rolled back.", exception);
        }
    }

    private String temporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        StringBuilder value = new StringBuilder("Ri1!");
        for (int i = 0; i < 12; i++) value.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        return value.toString();
    }

    public EmployeeDto getEmployee(Long employeeId, String authorization) {
        return employeeClient.getEmployee(authorization, employeeId);
    }

    public void setHrProfileStatus(Long employeeId, boolean active, String authorization) {
        employeeClient.setHrStatus(authorization, employeeId, new recruitment.dev.authservice.dto.StatusRequest(active));
    }

    private String resolveKeycloakRole(String authorization, Long roleId) {
        String roleName = employeeClient.getEmployeeRole(authorization, roleId).getName();
        if ("HR".equalsIgnoreCase(roleName)) return "HR";
        if ("MANAGER".equalsIgnoreCase(roleName)) return "MANAGER";
        return "EMPLOYEE";
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
