package recruitment.dev.authservice.feign;

import lombok.*;

import java.time.LocalDate;
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;


    private String keycloakId;


    private String firstName;


    private String lastName;


    private String email;


    private String phone;


    private LocalDate hireDate;


    private String position;



    private Long roleId;
    private Long departmentId;
    private Long companyId;
    private String companyName;
    private boolean active;
}
