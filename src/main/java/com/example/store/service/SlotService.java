package com.example.store.service;

import com.example.store.config.StoreProperties;
import com.example.store.domain.DeliverySlot;
import com.example.store.domain.StoreException;
import com.example.store.repository.DeliverySlotRepository;
import com.example.store.web.Api;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final DeliverySlotRepository slots;
    private final SlotHoldService holds;
    private final StoreProperties props;

    public SlotService(DeliverySlotRepository slots, SlotHoldService holds, StoreProperties props) {
        this.slots = slots;
        this.holds = holds;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<Api.SlotAvailability> availability(LocalDate date) {
        Instant from = date.atStartOfDay(props.zone()).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(props.zone()).toInstant();
        return slots.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAt(from, to).stream()
                .map(s -> {
                    int active = holds.activeHolds(s.getId());
                    int available = Math.max(0, s.getCapacity() - s.getBooked() - active);
                    return new Api.SlotAvailability(s.getId(), s.getStartsAt(), s.getEndsAt(),
                            s.getCapacity(), s.getBooked(), active, available);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Api.HoldResponse hold(long slotId, String customerId) {
        DeliverySlot slot = slots.findById(slotId)
                .orElseThrow(() -> StoreException.notFound("Delivery slot %d not found".formatted(slotId)));
        int free = slot.getCapacity() - slot.getBooked();
        Instant expiresAt = holds.tryHold(slotId, customerId, free)
                .orElseThrow(() -> StoreException.conflict("Delivery slot %d is fully booked".formatted(slotId)));
        return new Api.HoldResponse(slotId, customerId, expiresAt);
    }

    public void releaseHold(long slotId, String customerId) {
        holds.release(slotId, customerId);
    }
}
