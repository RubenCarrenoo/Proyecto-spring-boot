package com.coworkhub.business;

import com.coworkhub.dto.catalog.AdditionalServiceRequest;
import com.coworkhub.dto.catalog.AdditionalServiceResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/** Catálogo de servicios adicionales. */
@PreAuthorize("isAuthenticated()")
public interface AdditionalServiceCatalogService {

    List<AdditionalServiceResponse> findAll();

    AdditionalServiceResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    AdditionalServiceResponse create(AdditionalServiceRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    AdditionalServiceResponse update(Long id, AdditionalServiceRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);
}
