package com.campusbite.broadcast;

import com.campusbite.broadcast.dto.BroadcastMessageDto;
import com.campusbite.broadcast.dto.SetBroadcastRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class BroadcastMessageController {

    private final BroadcastMessageService service;

    public BroadcastMessageController(BroadcastMessageService service) {
        this.service = service;
    }

    /** Public -- every student page checks this on load, no auth needed to just read it. */
    @GetMapping("/api/broadcast/active")
    public BroadcastMessageDto active() {
        return service.getActive();
    }

    @PostMapping("/api/admin/broadcast")
    public BroadcastMessageDto set(@Valid @RequestBody SetBroadcastRequest request) {
        return service.setMessage(request.message());
    }

    @PostMapping("/api/admin/broadcast/clear")
    public void clear() {
        service.clear();
    }
}
