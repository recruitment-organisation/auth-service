package recruitment.dev.authservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.authservice.feign.EmployeeDto;

@Component
public class EmployeeClientFallbackFactory implements FallbackFactory<EmployeeClient> {

    @Override
    public EmployeeClient create(Throwable cause) {
        return dto -> {
            throw AuthFeignFallbacks.unavailable("employee-service", cause);
        };
    }
}
