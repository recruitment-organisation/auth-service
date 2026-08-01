package recruitment.dev.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import recruitment.dev.authservice.feign.CandidateDto;

@FeignClient(name = "candidate-service", fallbackFactory = CandidateClientFallbackFactory.class)
public interface CandidateClient {

    @PostMapping("/candidate/create")
    CandidateDto createCandidate(@RequestBody CandidateDto dto);

    @GetMapping("/candidate/me")
    CandidateDto getCurrentCandidate(@RequestHeader("Authorization") String authorization);
}
