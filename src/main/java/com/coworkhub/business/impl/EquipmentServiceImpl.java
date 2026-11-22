package com.coworkhub.business.impl;

import com.coworkhub.business.EquipmentService;
import com.coworkhub.domain.Equipment;
import com.coworkhub.dto.catalog.EquipmentRequest;
import com.coworkhub.dto.catalog.EquipmentResponse;
import com.coworkhub.exception.DuplicateResourceException;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.EquipmentRepository;
import com.coworkhub.persistence.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RoomRepository roomRepository;
    private final CatalogMapper mapper;

    public EquipmentServiceImpl(EquipmentRepository equipmentRepository, RoomRepository roomRepository,
                                CatalogMapper mapper) {
        this.equipmentRepository = equipmentRepository;
        this.roomRepository = roomRepository;
        this.mapper = mapper;
    }

    @Override
    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Override
    public EquipmentResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    @Override
    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        String name = request.nombre().trim();
        if (equipmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ya existe un equipamiento llamado '" + name + "'");
        }
        Equipment equipment = new Equipment();
        equipment.setName(name);
        return mapper.toResponse(equipmentRepository.save(equipment));
    }

    @Override
    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = get(id);
        String name = request.nombre().trim();
        if (equipmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ya existe un equipamiento llamado '" + name + "'");
        }
        equipment.setName(name);
        return mapper.toResponse(equipment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Equipment equipment = get(id);
        if (roomRepository.existsByEquipmentId(id)) {
            throw new ResourceInUseException("No se puede eliminar el equipamiento porque está asignado a una o más salas");
        }
        equipmentRepository.delete(equipment);
    }

    private Equipment get(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipamiento", id));
    }
}
