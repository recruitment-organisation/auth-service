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
            @Override public EmployeeDto createHrProfile(String authorization, Long companyId, recruitment.dev.authservice.feign.CreateHrProfileRequest request) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
            @Override public EmployeeDto getEmployee(String authorization, Long id) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
            @Override public EmployeeDto setHrStatus(String authorization, Long id, recruitment.dev.authservice.dto.StatusRequest request) {
                throw AuthFeignFallbacks.unavailable("employee-service", cause);
            }
        };
    }
}
