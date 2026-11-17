package com.coworkhub.persistence;

import com.coworkhub.domain.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
