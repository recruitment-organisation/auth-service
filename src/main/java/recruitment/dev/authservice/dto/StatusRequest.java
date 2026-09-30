package recruitment.dev.authservice.dto;

import jakarta.validation.constraints.NotNull;
public record StatusRequest(@NotNull Boolean active) {}
