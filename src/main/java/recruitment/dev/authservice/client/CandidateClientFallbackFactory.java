package recruitment.dev.authservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.authservice.feign.CandidateDto;

@Component
public class CandidateClientFallbackFactory implements FallbackFactory<CandidateClient> {

    @Override
    public CandidateClient create(Throwable cause) {
        return dto -> {
            throw AuthFeignFallbacks.unavailable("candidate-service", cause);
        };
    }
}
