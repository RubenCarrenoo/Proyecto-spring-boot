package com.coworkhub.business;

import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.dto.member.ConsumptionResponse;
import com.coworkhub.dto.member.MemberRequest;
import com.coworkhub.dto.member.MemberResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.YearMonth;
import java.util.List;

/**
 * Gestión de miembros. Registro y administración: ADMIN y RECEPTIONIST.
 * Un MEMBER solo puede consultar su propio detalle y consumo (comprobado en la implementación).
 */
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public interface MemberService {

    List<MemberResponse> findAll(String name, MemberStatus status);

    @PreAuthorize("isAuthenticated()")
    MemberResponse findById(Long id);

    MemberResponse create(MemberRequest request);

    MemberResponse update(Long id, MemberRequest request);

    MemberResponse suspend(Long id);

    MemberResponse activate(Long id);

    void delete(Long id);

    @PreAuthorize("isAuthenticated()")
    ConsumptionResponse getMonthlyConsumption(Long id, YearMonth month);
}
