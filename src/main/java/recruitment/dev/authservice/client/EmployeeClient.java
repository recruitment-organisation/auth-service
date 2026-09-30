package recruitment.dev.authservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import recruitment.dev.authservice.confi.FeignSecurityConfig;
import recruitment.dev.authservice.feign.EmployeeDto;
import recruitment.dev.authservice.feign.EmployeeRoleDto;
import recruitment.dev.authservice.feign.CreateHrProfileRequest;
import recruitment.dev.authservice.dto.StatusRequest;

@FeignClient(name = "employee-service",
        configuration = FeignSecurityConfig.class,
        fallbackFactory = EmployeeClientFallbackFactory.class
)

public interface EmployeeClient {
    @PostMapping("/employee/create")
    EmployeeDto createEmployee(@RequestHeader("Authorization") String authorization, @RequestBody EmployeeDto employeeDto);
    @GetMapping("/employee/me")
    EmployeeDto getCurrentEmployee(@RequestHeader("Authorization") String authorization);
    @GetMapping("/employee-role/get/{id}")
    EmployeeRoleDto getEmployeeRole(@RequestHeader("Authorization") String authorization, @org.springframework.web.bind.annotation.PathVariable("id") Long id);
    @PostMapping("/admin/companies/{companyId}/hr-profile")
    EmployeeDto createHrProfile(@RequestHeader("Authorization") String authorization, @org.springframework.web.bind.annotation.PathVariable("companyId") Long companyId, @RequestBody CreateHrProfileRequest request);
    @org.springframework.web.bind.annotation.GetMapping("/employee/get/{id}")
    EmployeeDto getEmployee(@RequestHeader("Authorization") String authorization, @org.springframework.web.bind.annotation.PathVariable("id") Long id);
    @org.springframework.web.bind.annotation.PatchMapping("/admin/hr/{id}/status")
    EmployeeDto setHrStatus(@RequestHeader("Authorization") String authorization, @org.springframework.web.bind.annotation.PathVariable("id") Long id, @RequestBody StatusRequest request);
}
