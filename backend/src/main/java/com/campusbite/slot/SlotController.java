package com.campusbite.slot;

import com.campusbite.slot.dto.SlotCapacityDto;
import com.campusbite.slot.dto.SlotCapacityRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/slots")
public class SlotController {

    private final SlotCapacityRepository slotCapacityRepository;
    private final int windowMinutes;

    public SlotController(SlotCapacityRepository slotCapacityRepository,
                          @Value("${campusbite.slot.default-window-minutes}") int windowMinutes) {
        this.slotCapacityRepository = slotCapacityRepository;
        this.windowMinutes = windowMinutes;
    }

    @GetMapping
    public ResponseEntity<List<SlotCapacityDto>> list() {
        return ResponseEntity.ok(slotCapacityRepository.findAll().stream().map(SlotCapacityDto::from).toList());
    }

    @PostMapping
    public ResponseEntity<SlotCapacityDto> upsert(@Valid @RequestBody SlotCapacityRequest request) {
        // Windows only ever land on multiples of the global window size (see
        // SlotAllocationService) -- silently round anything else to the
        // nearest valid boundary so an admin can never configure a capacity
        // that the allocator would never actually check.
        int rounded = Math.round(request.windowStartMinuteOfDay() / (float) windowMinutes) * windowMinutes;

        SlotCapacity capacity = slotCapacityRepository.findByWindowStartMinuteOfDay(rounded)
                .orElseGet(SlotCapacity::new);
        capacity.setWindowStartMinuteOfDay(rounded);
        capacity.setMaxOrders(request.maxOrders());
        return ResponseEntity.ok(SlotCapacityDto.from(slotCapacityRepository.save(capacity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        slotCapacityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}