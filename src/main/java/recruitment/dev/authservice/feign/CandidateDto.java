package recruitment.dev.authservice.feign;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {

    private Long id;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String location;
    private Boolean available;
    private String linkedinUrl;
    private String githubUrl;
}