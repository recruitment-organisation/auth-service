package recruitment.dev.authservice.feign;

import lombok.*;

import java.time.LocalDate;
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private LocalDate hireDate;
    private String position;
    private Long roleId;
    private Long departmentId;
}