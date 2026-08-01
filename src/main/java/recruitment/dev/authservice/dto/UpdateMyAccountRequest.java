package recruitment.dev.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyAccountRequest {

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;
}
