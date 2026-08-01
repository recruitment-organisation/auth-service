package recruitment.dev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken is required")
    @Size(max = 4096, message = "refreshToken is invalid")
    private String refreshToken;
}
