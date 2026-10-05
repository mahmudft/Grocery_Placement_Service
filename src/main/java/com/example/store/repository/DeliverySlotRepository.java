package com.example.store.repository;

import com.example.store.domain.DeliverySlot;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Long> {

    @Modifying
    @Query("update DeliverySlot s set s.booked = s.booked + 1 where s.id = :id and s.booked < s.capacity")
    int reserveCapacity(@Param("id") long id);

    List<DeliverySlot> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(Instant from, Instant to);
}
