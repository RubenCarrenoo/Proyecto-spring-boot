package com.coworkhub.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acceso al usuario autenticado y comprobaciones de propiedad de recursos (RN-13).
 * Se usa desde la capa de negocio: la autorización no depende solo de los controladores.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }
        return user;
    }

    /**
     * ADMIN y RECEPTIONIST pueden operar sobre cualquier miembro; un MEMBER solo sobre sí mismo.
     */
    public void checkCanAccessMember(Long memberId) {
        AuthenticatedUser user = get();
        if (user.isStaff()) {
            return;
        }
        if (user.getMemberId() == null || !user.getMemberId().equals(memberId)) {
            throw new AccessDeniedException("No puede acceder a información de otro miembro");
        }
    }
}
