package com.coworkhub.business.impl;

import com.coworkhub.business.AuthService;
import com.coworkhub.dto.auth.LoginRequest;
import com.coworkhub.dto.auth.LoginResponse;
import com.coworkhub.dto.auth.ProfileResponse;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.security.AuthenticatedUser;
import com.coworkhub.security.CurrentUserProvider;
import com.coworkhub.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUser;
    private final MemberRepository memberRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtService jwtService,
                           CurrentUserProvider currentUser, MemberRepository memberRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.memberRepository = memberRepository;
    }

    /** Valida usuario y contraseña (BCrypt). Si fallan, Spring lanza BadCredentialsException -> 401. */
    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usuario().trim(), request.password()));
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMinutes(), user.getUsername(),
                user.getRole().name(), user.getMemberId());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse profile() {
        AuthenticatedUser user = currentUser.get();
        String memberName = user.getMemberId() == null ? null
                : memberRepository.findById(user.getMemberId()).map(m -> m.getFullName()).orElse(null);
        return new ProfileResponse(user.getUsername(), user.getRole().name(), user.getMemberId(), memberName);
    }
}
