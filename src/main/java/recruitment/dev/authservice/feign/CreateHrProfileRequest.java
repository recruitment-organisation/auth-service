package recruitment.dev.authservice.feign;

public record CreateHrProfileRequest(String keycloakId, String firstName, String lastName, String email, String phone, Boolean active) {}
