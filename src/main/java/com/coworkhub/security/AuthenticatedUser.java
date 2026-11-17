package com.coworkhub.security;

import com.coworkhub.domain.UserAccount;
import com.coworkhub.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal de Spring Security. Además del usuario y el rol guarda el id del miembro vinculado,
 * necesario para el control de propiedad de recursos (RN-13).
 */
public class AuthenticatedUser implements UserDetails {

    private final Long accountId;
    private final String username;
    private final String password;
    private final Role role;
    private final Long memberId;

    public AuthenticatedUser(Long accountId, String username, String password, Role role, Long memberId) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.memberId = memberId;
    }

    public static AuthenticatedUser from(UserAccount account) {
        Long memberId = account.getMember() == null ? null : account.getMember().getId();
        return new AuthenticatedUser(account.getId(), account.getUsername(), account.getPassword(),
                account.getRole(), memberId);
    }

    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.RECEPTIONIST;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Role getRole() {
        return role;
    }

    public Long getMemberId() {
        return memberId;
    }
}
