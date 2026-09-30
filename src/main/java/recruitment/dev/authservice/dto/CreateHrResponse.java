package recruitment.dev.authservice.dto;

public record CreateHrResponse(String keycloakUserId, Long employeeId, String email, String temporaryPassword, Long companyId) {}
