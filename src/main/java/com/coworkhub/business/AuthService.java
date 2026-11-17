package com.coworkhub.business;

import com.coworkhub.dto.auth.LoginRequest;
import com.coworkhub.dto.auth.LoginResponse;
import com.coworkhub.dto.auth.ProfileResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    @PreAuthorize("isAuthenticated()")
    ProfileResponse profile();
}
