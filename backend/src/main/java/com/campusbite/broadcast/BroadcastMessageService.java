package com.campusbite.broadcast;

import com.campusbite.broadcast.dto.BroadcastMessageDto;
import com.campusbite.common.ws.BroadcastService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BroadcastMessageService {

    private final BroadcastMessageRepository repository;
    private final BroadcastService wsBroadcastService;

    public BroadcastMessageService(BroadcastMessageRepository repository, BroadcastService wsBroadcastService) {
        this.repository = repository;
        this.wsBroadcastService = wsBroadcastService;
    }

    public BroadcastMessageDto getActive() {
        return repository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(BroadcastMessageDto::from)
                .orElse(new BroadcastMessageDto(null, null, false));
    }

    @Transactional
    public BroadcastMessageDto setMessage(String message) {
        // Deactivate anything currently showing, then create a fresh active one.
        repository.findFirstByActiveTrueOrderByUpdatedAtDesc().ifPresent(existing -> {
            existing.setActive(false);
            repository.save(existing);
        });
        BroadcastMessage created = repository.save(new BroadcastMessage(message, true));
        wsBroadcastService.broadcastMenuUpdate(); // reuse the generic "something changed" ping; students poll on load anyway
        return BroadcastMessageDto.from(created);
    }

    @Transactional
    public void clear() {
        repository.findFirstByActiveTrueOrderByUpdatedAtDesc().ifPresent(existing -> {
            existing.setActive(false);
            repository.save(existing);
        });
    }
}
