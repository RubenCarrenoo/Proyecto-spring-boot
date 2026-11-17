package com.coworkhub.business.impl;

import com.coworkhub.business.MembershipPlanService;
import com.coworkhub.domain.MembershipPlan;
import com.coworkhub.dto.catalog.MembershipPlanRequest;
import com.coworkhub.dto.catalog.MembershipPlanResponse;
import com.coworkhub.exception.DuplicateResourceException;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.persistence.MembershipPlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MembershipPlanServiceImpl implements MembershipPlanService {

    private final MembershipPlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final CatalogMapper mapper;

    public MembershipPlanServiceImpl(MembershipPlanRepository planRepository, MemberRepository memberRepository,
                                     CatalogMapper mapper) {
        this.planRepository = planRepository;
        this.memberRepository = memberRepository;
        this.mapper = mapper;
    }

    @Override
    public List<MembershipPlanResponse> findAll() {
        return planRepository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Override
    public MembershipPlanResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    @Override
    @Transactional
    public MembershipPlanResponse create(MembershipPlanRequest request) {
        String name = request.nombre().trim();
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ya existe un plan llamado '" + name + "'");
        }
        MembershipPlan plan = new MembershipPlan();
        mapper.apply(request, plan);
        return mapper.toResponse(planRepository.save(plan));
    }

    @Override
    @Transactional
    public MembershipPlanResponse update(Long id, MembershipPlanRequest request) {
        MembershipPlan plan = get(id);
        String name = request.nombre().trim();
        if (planRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ya existe un plan llamado '" + name + "'");
        }
        mapper.apply(request, plan);
        return mapper.toResponse(plan);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MembershipPlan plan = get(id);
        if (memberRepository.existsByPlanId(id)) {
            throw new ResourceInUseException("No se puede eliminar el plan porque tiene miembros asociados");
        }
        planRepository.delete(plan);
    }

    private MembershipPlan get(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }
}
