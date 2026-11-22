package com.coworkhub.business.impl;

import com.coworkhub.business.AdditionalServiceCatalogService;
import com.coworkhub.domain.AdditionalService;
import com.coworkhub.dto.catalog.AdditionalServiceRequest;
import com.coworkhub.dto.catalog.AdditionalServiceResponse;
import com.coworkhub.exception.DuplicateResourceException;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.AdditionalServiceRepository;
import com.coworkhub.persistence.BookingItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdditionalServiceCatalogServiceImpl implements AdditionalServiceCatalogService {

    private final AdditionalServiceRepository serviceRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CatalogMapper mapper;

    public AdditionalServiceCatalogServiceImpl(AdditionalServiceRepository serviceRepository,
                                               BookingItemRepository bookingItemRepository,
                                               CatalogMapper mapper) {
        this.serviceRepository = serviceRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.mapper = mapper;
    }

    @Override
    public List<AdditionalServiceResponse> findAll() {
        return serviceRepository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Override
    public AdditionalServiceResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    @Override
    @Transactional
    public AdditionalServiceResponse create(AdditionalServiceRequest request) {
        String name = request.nombre().trim();
        if (serviceRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ya existe un servicio adicional llamado '" + name + "'");
        }
        AdditionalService service = new AdditionalService();
        mapper.apply(request, service);
        return mapper.toResponse(serviceRepository.save(service));
    }

    /** Cambiar el precio no altera reservas existentes: cada ítem guarda appliedUnitPrice. */
    @Override
    @Transactional
    public AdditionalServiceResponse update(Long id, AdditionalServiceRequest request) {
        AdditionalService service = get(id);
        String name = request.nombre().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ya existe un servicio adicional llamado '" + name + "'");
        }
        mapper.apply(request, service);
        return mapper.toResponse(service);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdditionalService service = get(id);
        if (bookingItemRepository.existsByServiceId(id)) {
            throw new ResourceInUseException("No se puede eliminar el servicio porque está incluido en reservas");
        }
        serviceRepository.delete(service);
    }

    private AdditionalService get(Long id) {
        return serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Servicio adicional", id));
    }
}
