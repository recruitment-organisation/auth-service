package recruitment.dev.authservice.feign;

import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    @NotBlank @Size(max = 50) private String firstName;
    @NotBlank @Size(max = 50) private String lastName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 8, max = 128) private String password;
    @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,15}$") private String phone;
    @NotNull private LocalDate hireDate;
    @NotBlank @Size(max = 100) private String position;
    @NotNull @Positive private Long roleId;
    @NotNull @Positive private Long departmentId;
}
