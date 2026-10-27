package recruitment.dev.authservice.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ApplicationRoles {

    private static final Set<String> ALLOWED = Set.of("ADMIN", "HR", "MANAGER", "EMPLOYEE", "CANDIDATE");

    private ApplicationRoles() {
    }

    public static List<String> filter(Collection<?> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(ALLOWED::contains)
                .distinct()
                .toList();
    }
}
