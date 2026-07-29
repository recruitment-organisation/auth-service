package recruitment.dev.authservice.client;

import org.springframework.http.HttpStatus;
import recruitment.dev.authservice.exception.AuthException;

final class AuthFeignFallbacks {

    private AuthFeignFallbacks() {
    }

    static AuthException unavailable(String dependency, Throwable cause) {
        if (cause instanceof AuthException exception) {
            return exception;
        }
        return new AuthException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Required dependency is unavailable: " + dependency,
                cause
        );
    }
}
