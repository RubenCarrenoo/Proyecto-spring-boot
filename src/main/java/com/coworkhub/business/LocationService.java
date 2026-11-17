package com.coworkhub.business;

import com.coworkhub.dto.catalog.LocationRequest;
import com.coworkhub.dto.catalog.LocationResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("isAuthenticated()")
public interface LocationService {

    List<LocationResponse> findAll();

    LocationResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    LocationResponse create(LocationRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    LocationResponse update(Long id, LocationRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);
}
