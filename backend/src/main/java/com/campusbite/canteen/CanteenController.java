package com.campusbite.canteen;

import com.campusbite.canteen.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CanteenController {
    private final CanteenService service;
    public CanteenController(CanteenService service) { this.service = service; }

    @GetMapping("/api/canteen/status")
    public CanteenSettingsDto status() { return service.publicSettings(); }

    @GetMapping("/api/admin/canteen")
    public CanteenSettingsDto settings() { return service.publicSettings(); }

    @PutMapping("/api/admin/canteen")
    public CanteenSettingsDto update(@Valid @RequestBody CanteenSettingsRequest request) { return service.updateSettings(request); }

    @GetMapping("/api/admin/canteen/closures")
    public List<CanteenClosureDto> closures() { return service.closures(); }

    @PostMapping("/api/admin/canteen/closures")
    public CanteenClosureDto addClosure(@Valid @RequestBody CanteenClosureRequest request) { return service.addClosure(request); }

    @DeleteMapping("/api/admin/canteen/closures/{id}")
    public ResponseEntity<Void> removeClosure(@PathVariable Long id) { service.removeClosure(id); return ResponseEntity.noContent().build(); }
}
