package com.example.store.web;

import com.example.store.service.SlotService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public List<Api.SlotAvailability> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.availability(date);
    }

    @PostMapping("/{slotId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public Api.HoldResponse hold(@PathVariable long slotId, @Valid @RequestBody Api.HoldRequest request) {
        return slotService.hold(slotId, request.customerId());
    }

    @DeleteMapping("/{slotId}/holds/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable long slotId, @PathVariable String customerId) {
        slotService.releaseHold(slotId, customerId);
    }
}
