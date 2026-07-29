package recruitment.dev.authservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import recruitment.dev.authservice.confi.FeignSecurityConfig;
import recruitment.dev.authservice.feign.EmployeeDto;

@FeignClient(name = "employee-service",
        configuration = FeignSecurityConfig.class,
        fallbackFactory = EmployeeClientFallbackFactory.class
)

public interface EmployeeClient {
    @PostMapping("/employee/create")
    EmployeeDto createEmployee(@RequestBody EmployeeDto employeeDto);
}
