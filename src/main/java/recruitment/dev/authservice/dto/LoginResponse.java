package recruitment.dev.authservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private String username;

    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private List<String> roles;

    /** ID of the candidate or employee domain profile associated with this account. */
    private Long userId;
}
