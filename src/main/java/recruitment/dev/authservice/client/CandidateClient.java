package recruitment.dev.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import recruitment.dev.authservice.feign.CandidateDto;

@FeignClient(name = "candidate-service")
public interface CandidateClient {

    @PostMapping("/candidate/create")
    CandidateDto createCandidate(@RequestBody CandidateDto dto);
}