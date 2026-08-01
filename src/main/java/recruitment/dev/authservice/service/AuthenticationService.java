package recruitment.dev.authservice.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import recruitment.dev.authservice.confi.KeycloakAdminProperties;
import recruitment.dev.authservice.client.CandidateClient;
import recruitment.dev.authservice.client.EmployeeClient;
import recruitment.dev.authservice.dto.LoginRequest;
import recruitment.dev.authservice.dto.LoginResponse;
import recruitment.dev.authservice.dto.LogoutRequest;
import recruitment.dev.authservice.dto.RefreshTokenRequest;
import recruitment.dev.authservice.exception.AuthException;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakAdminProperties properties;
    private final CandidateClient candidateClient;
    private final EmployeeClient employeeClient;

    public LoginResponse login(LoginRequest request) {
        try (Keycloak keycloakUser = KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .username(request.getUsername())
                .password(request.getPassword())
                .grantType(OAuth2Constants.PASSWORD)
                .scope("openid profile email")
                .build()) {

            String accessToken = keycloakUser.tokenManager().getAccessTokenString();
            String refreshToken = keycloakUser.tokenManager().getAccessToken().getRefreshToken();
            Long expiresIn = keycloakUser.tokenManager().getAccessToken().getExpiresIn();

            return buildLoginResponse(accessToken, refreshToken, expiresIn);

        } catch (Exception ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password", ex);
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        MultiValueMap<String, String> formData = baseClientFormData();
        formData.add("grant_type", OAuth2Constants.REFRESH_TOKEN);
        formData.add("refresh_token", request.getRefreshToken());

        try {
            Map<String, Object> tokenResponse = webClientBuilder.build()
                    .post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("Keycloak refresh token failed")
                                    .map(error -> new AuthException(HttpStatus.UNAUTHORIZED,
                                            "Invalid or expired refresh token")))
                    .bodyToMono(Map.class)
                    .block();

            if (tokenResponse == null || !(tokenResponse.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Unable to refresh token.");
            }

            String newRefreshToken = tokenResponse.get("refresh_token") instanceof String token && !token.isBlank()
                    ? token : request.getRefreshToken();
            Long expiresIn = tokenResponse.get("expires_in") instanceof Number value
                    ? value.longValue() : null;
            if (expiresIn == null || expiresIn <= 0) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh-token response.");
            }

            return buildLoginResponse(accessToken, newRefreshToken, expiresIn);

        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token", ex);
        }
    }

    public void logout(LogoutRequest request) {
        MultiValueMap<String, String> formData = baseClientFormData();
        formData.add("refresh_token", request.getRefreshToken());

        webClientBuilder.build()
                .post()
                .uri(properties.getLogoutUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("Keycloak logout failed")
                                .map(error -> new AuthException(HttpStatus.BAD_REQUEST, error)))
                .toBodilessEntity()
                .block();
    }

    @SuppressWarnings("unchecked")
    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, Long expiresIn)
            throws java.text.ParseException {

        SignedJWT jwt = SignedJWT.parse(accessToken);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setId(claims.getSubject());
        response.setUsername(claims.getStringClaim("preferred_username"));
        response.setEmail(claims.getStringClaim("email"));
        response.setFirstName(claims.getStringClaim("given_name"));
        response.setLastName(claims.getStringClaim("family_name"));

        Map<String, Object> realmAccess = (Map<String, Object>) claims.getClaim("realm_access");
        Object roles = realmAccess == null ? null : realmAccess.get("roles");
        response.setRoles(roles instanceof List<?> roleList
                ? roleList.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : Collections.emptyList());
        populateEntityIds(response, accessToken);

        return response;
    }

    private void populateEntityIds(LoginResponse response, String accessToken) {
        String authorization = "Bearer " + accessToken;
        try {
            if (response.getRoles().contains("CANDIDATE")) {
                response.setUserId(candidateClient.getCurrentCandidate(authorization).getId());
            } else if (response.getRoles().stream().anyMatch(role -> role.equals("EMPLOYEE") || role.equals("HR") || role.equals("MANAGER"))) {
                response.setUserId(employeeClient.getCurrentEmployee(authorization).getId());
            }
        } catch (RuntimeException ignored) {
            // A Keycloak account can exist before its optional domain profile is created.
        }
    }

    private MultiValueMap<String, String> baseClientFormData() {
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", properties.getClientId());
        if (properties.hasClientSecret()) {
            formData.add("client_secret", properties.getClientSecret());
        }
        return formData;
    }
}
