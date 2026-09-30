package com.campusbite.kitchenload;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KitchenLoadController {

    private final KitchenLoadService kitchenLoadService;

    public KitchenLoadController(KitchenLoadService kitchenLoadService) {
        this.kitchenLoadService = kitchenLoadService;
    }

    @GetMapping("/api/kitchen-load/current")
    public ResponseEntity<KitchenLoadDto> current() {
        return ResponseEntity.ok(kitchenLoadService.getLatest());
    }
}
