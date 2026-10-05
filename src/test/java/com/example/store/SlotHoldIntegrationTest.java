package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.store.domain.DeliverySlot;
import com.example.store.domain.StoreException;
import com.example.store.repository.DeliverySlotRepository;
import com.example.store.service.SlotHoldService;
import com.example.store.service.SlotService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SlotHoldIntegrationTest extends AbstractIntegrationTest {

    @Autowired SlotService slotService;
    @Autowired SlotHoldService holds;
    @Autowired DeliverySlotRepository slots;

    @Test
    void holdsAreCappedAtFreeCapacityAndReleasable() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        long slotId = slots.save(new DeliverySlot(start, start.plus(2, ChronoUnit.HOURS), 2)).getId();

        slotService.hold(slotId, "alice");
        slotService.hold(slotId, "bob");

        assertThatThrownBy(() -> slotService.hold(slotId, "carol"))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("fully booked");
        assertThat(holds.activeHolds(slotId)).isEqualTo(2);

        // re-holding as an existing holder is idempotent and does not consume extra capacity
        assertThatCode(() -> slotService.hold(slotId, "alice")).doesNotThrowAnyException();
        assertThat(holds.activeHolds(slotId)).isEqualTo(2);

        slotService.releaseHold(slotId, "alice");
        assertThatCode(() -> slotService.hold(slotId, "carol")).doesNotThrowAnyException();
    }
}
