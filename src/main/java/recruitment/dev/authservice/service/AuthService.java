package recruitment.dev.authservice.service;

import jakarta.validation.Valid;
import recruitment.dev.authservice.dto.*;
import recruitment.dev.authservice.feign.CreateEmployeeRequest;

public interface AuthService {
    LoginResponse login(@Valid LoginRequest request);


    void logout(@Valid LogoutRequest request);

    String createUser( @Valid CreateUserRequest request);

    void deleteUser(String userId) ;
    LoginResponse refreshToken(RefreshTokenRequest request);
    CreateUserResponse createEmployee(CreateEmployeeRequest request);
    void updateUserRole(String userId, UpdateUserRoleRequest request);

}
