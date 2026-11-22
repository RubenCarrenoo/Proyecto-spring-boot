package com.coworkhub.business;

import com.coworkhub.dto.catalog.MembershipPlanRequest;
import com.coworkhub.dto.catalog.MembershipPlanResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("isAuthenticated()")
public interface MembershipPlanService {

    List<MembershipPlanResponse> findAll();

    MembershipPlanResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    MembershipPlanResponse create(MembershipPlanRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    MembershipPlanResponse update(Long id, MembershipPlanRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);
}
