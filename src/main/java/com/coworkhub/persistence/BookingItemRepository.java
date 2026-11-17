package com.coworkhub.persistence;

import com.coworkhub.domain.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    boolean existsByServiceId(Long serviceId);
}
