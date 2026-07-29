package recruitment.dev.authservice.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import recruitment.dev.authservice.confi.KeycloakAdminProperties;
import recruitment.dev.authservice.exception.AuthException;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserAdminService {

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;

    public boolean userExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return !keycloak.realm(properties.getRealm())
                .users()
                .searchByEmail(email.trim(), true)
                .isEmpty();
    }

    public boolean usernameExists(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return !keycloak.realm(properties.getRealm())
                .users()
                .searchByUsername(username.trim(), true)
                .isEmpty();
    }

    public String generateUsername(String firstName, String lastName) {
        String base = capitalize(firstName) + capitalize(lastName);
        String candidate = base;
        int suffix = 1;
        while (usernameExists(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim().toLowerCase();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    public UserRepresentation buildUserRepresentation(
            String email, String firstName, String lastName, String password, String username) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }

    public String createKeycloakUser(UserRepresentation user) {
        try (Response response = keycloak.realm(properties.getRealm())
                .users()
                .create(user)) {

            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new AuthException(
                        HttpStatus.BAD_GATEWAY,
                        "User creation failed. Status: " + response.getStatus()
                );
            }

            return CreatedResponseUtil.getCreatedId(response);

        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to create user.", ex);
        }
    }


    public void deleteUser(String userId) {
        try (Response response = keycloak.realm(properties.getRealm())
                .users()
                .delete(userId)) {

            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                throw new AuthException(HttpStatus.BAD_GATEWAY, "Unable to delete user.");
            }
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Error while deleting user.", ex);
        }
    }
    public void assignRole(String userId, String roleName) {

        RoleRepresentation role =
                keycloak.realm(properties.getRealm())
                        .roles()
                        .get(roleName)
                        .toRepresentation();


        keycloak.realm(properties.getRealm())
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }

    public void updateUserRole(
            String userId,
            String oldRoleName,
            String newRoleName
    ) {
        if (userId == null || userId.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "User identifier is required");
        }

        if (oldRoleName == null || oldRoleName.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Old role is required");
        }

        if (newRoleName == null || newRoleName.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "New role is required");
        }

        try {
            RoleRepresentation oldRole = keycloak.realm(properties.getRealm())
                    .roles()
                    .get(oldRoleName)
                    .toRepresentation();

            RoleRepresentation newRole = keycloak.realm(properties.getRealm())
                    .roles()
                    .get(newRoleName)
                    .toRepresentation();

            var realmLevelRoles = keycloak.realm(properties.getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel();

            realmLevelRoles.remove(List.of(oldRole));
            realmLevelRoles.add(List.of(newRole));
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to update user role.",
                    exception
            );
        }
    }

    public void rollbackKeycloakUser(String keycloakUserId) {

        try {

            keycloak.realm(properties.getRealm())
                    .users()
                    .get(keycloakUserId)
                    .remove();

            log.info(
                    "Keycloak user {} rolled back successfully",
                    keycloakUserId
            );


        } catch (jakarta.ws.rs.NotFoundException e) {

            log.warn(
                    "Keycloak user {} already deleted",
                    keycloakUserId
            );


        } catch (Exception ex) {

            log.error(
                    "CRITICAL: Failed to rollback Keycloak user {}",
                    keycloakUserId,
                    ex
            );
        }
    }
}
