package com.coworkhub.business;

import com.coworkhub.dto.catalog.EquipmentRequest;
import com.coworkhub.dto.catalog.EquipmentResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("isAuthenticated()")
public interface EquipmentService {

    List<EquipmentResponse> findAll();

    EquipmentResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    EquipmentResponse create(EquipmentRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    EquipmentResponse update(Long id, EquipmentRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);
}
