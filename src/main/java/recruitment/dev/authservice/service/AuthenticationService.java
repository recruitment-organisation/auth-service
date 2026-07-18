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
import recruitment.dev.authservice.dto.LoginRequest;
import recruitment.dev.authservice.dto.LoginResponse;
import recruitment.dev.authservice.dto.LogoutRequest;
import recruitment.dev.authservice.dto.RefreshTokenRequest;
import recruitment.dev.authservice.exception.AuthException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final WebClient.Builder webClientBuilder;
    private final KeycloakAdminProperties properties;

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

            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Unable to refresh token.");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String newRefreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            return buildLoginResponse(accessToken, newRefreshToken,
                    expiresIn != null ? expiresIn.longValue() : null);

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
        response.setUsername(claims.getStringClaim("preferred_username"));
        response.setEmail(claims.getStringClaim("email"));

        Map<String, Object> realmAccess = (Map<String, Object>) claims.getClaim("realm_access");
        if (realmAccess != null) {
            response.setRoles((List<String>) realmAccess.get("roles"));
        }

        return response;
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