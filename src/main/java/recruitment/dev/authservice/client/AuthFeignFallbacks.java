package recruitment.dev.authservice.client;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import recruitment.dev.authservice.exception.AuthException;

final class AuthFeignFallbacks {

    private AuthFeignFallbacks() {
    }

    static AuthException unavailable(String dependency, Throwable cause) {
        if (cause instanceof AuthException exception) {
            return exception;
        }
        if (cause instanceof FeignException exception && exception.status() >= 400 && exception.status() < 500) {
            HttpStatus status = HttpStatus.resolve(exception.status());
            return new AuthException(
                    status == null ? HttpStatus.BAD_REQUEST : status,
                    "Request rejected by " + dependency,
                    cause
            );
        }
        return new AuthException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Required dependency is unavailable: " + dependency,
                cause
        );
    }
}
