package com.coworkhub.security;

import com.coworkhub.persistence.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carga las cuentas desde la tabla user_accounts para Spring Security. */
@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public UserAccountDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsername(username)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
