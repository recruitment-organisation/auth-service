package recruitment.dev.authservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.authservice.feign.EmployeeDto;

@Component
public class EmployeeClientFallbackFactory implements FallbackFactory<EmployeeClient> {

    @Override
    public EmployeeClient create(Throwable cause) {
        return new EmployeeClient() {
            @Override public EmployeeDto createEmployee(String authorization, EmployeeDto dto) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
            @Override public EmployeeDto getCurrentEmployee(String authorization) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
            @Override public recruitment.dev.authservice.feign.EmployeeRoleDto getEmployeeRole(String authorization, Long id) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
        };
    }
}
