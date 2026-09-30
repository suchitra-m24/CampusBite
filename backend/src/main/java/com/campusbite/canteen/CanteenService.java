package com.campusbite.canteen;

import com.campusbite.canteen.dto.CanteenClosureDto;
import com.campusbite.canteen.dto.CanteenClosureRequest;
import com.campusbite.canteen.dto.CanteenSettingsDto;
import com.campusbite.canteen.dto.CanteenSettingsRequest;
import com.campusbite.common.exception.BadRequestException;
import com.campusbite.common.exception.ConflictException;
import com.campusbite.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class CanteenService {
    private final CanteenSettingsRepository settingsRepository;
    private final CanteenClosureRepository closureRepository;
    private final ZoneId campusZone;

    public CanteenService(CanteenSettingsRepository settingsRepository,
                          CanteenClosureRepository closureRepository,
                          @org.springframework.beans.factory.annotation.Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.settingsRepository = settingsRepository;
        this.closureRepository = closureRepository;
        this.campusZone = ZoneId.of(timeZone);
    }

    public CanteenSettings getSettings() {
        return settingsRepository.findTopByOrderByIdAsc().orElseGet(() -> {
            CanteenSettings s = new CanteenSettings();
            return settingsRepository.save(s);
        });
    }

    public CanteenSettingsDto publicSettings() { return CanteenSettingsDto.from(getSettings()); }

    public boolean isClosedOn(LocalDate date) { return closureRepository.existsByClosureDate(date); }

    /** Returns whether a complete pickup window is inside operating hours, independent of the ordering toggle. */
    public boolean isWithinOperatingHours(Instant start, Instant end) {
        CanteenSettings s = getSettings();
        ZonedDateTime a = start.atZone(campusZone);
        ZonedDateTime b = end.atZone(campusZone);
        if (isClosedOn(a.toLocalDate())) return false;
        if (a.toLocalDate().isAfter(b.toLocalDate())) return false;
        LocalTime open = s.getOpeningTime();
        LocalTime close = s.getClosingTime();
        return !a.toLocalTime().isBefore(open) && !b.toLocalTime().isAfter(close);
    }

    /** Returns whether a new order may use the complete pickup window. */
    public boolean isPickupWindowAllowed(Instant start, Instant end) {
        return getSettings().isOrderingEnabled() && isWithinOperatingHours(start, end);
    }

    /** Moves a candidate slot to the next valid operating window. */
    public Instant nextAllowedWindowStart(Instant candidate, int windowMinutes) {
        CanteenSettings s = getSettings();
        ZoneId zone = campusZone;
        ZonedDateTime z = candidate.atZone(zone).withSecond(0).withNano(0);
        for (int i = 0; i < 7 * 24 * 60 / Math.max(1, windowMinutes) + 2; i++) {
            LocalDate d = z.toLocalDate();
            if (!isClosedOn(d) && s.isOrderingEnabled()) {
                if (z.toLocalTime().isBefore(s.getOpeningTime())) {
                    z = ZonedDateTime.of(d, s.getOpeningTime(), zone);
                }
                LocalTime end = z.toLocalTime().plusMinutes(windowMinutes);
                if (!z.toLocalTime().isBefore(s.getOpeningTime()) && !end.isAfter(s.getClosingTime())) return z.toInstant();
            }
            z = z.plusDays(1).with(s.getOpeningTime());
        }
        throw new BadRequestException("The canteen has no available pickup window in the next 7 days.");
    }

    @Transactional
    public CanteenSettingsDto updateSettings(CanteenSettingsRequest request) {
        LocalTime open = parseTime(request.openingTime());
        LocalTime close = parseTime(request.closingTime());
        if (!open.isBefore(close)) throw new BadRequestException("Opening time must be before closing time");
        CanteenSettings s = getSettings();
        s.setCanteenName(request.canteenName().trim());
        s.setOpeningTime(open);
        s.setClosingTime(close);
        s.setOrderingEnabled(request.orderingEnabled());
        return CanteenSettingsDto.from(settingsRepository.save(s));
    }

    public List<CanteenClosureDto> closures() {
        return closureRepository.findAllByClosureDateGreaterThanEqualOrderByClosureDateAsc(LocalDate.now(campusZone)).stream().map(CanteenClosureDto::from).toList();
    }

    @Transactional
    public CanteenClosureDto addClosure(CanteenClosureRequest request) {
        LocalDate date = parseDate(request.date());
        if (date.isBefore(LocalDate.now(campusZone))) {
            throw new BadRequestException("Closure date cannot be in the past");
        }
        if (closureRepository.existsByClosureDate(date)) throw new ConflictException("The canteen is already marked closed on " + date);
        CanteenClosure c = new CanteenClosure();
        c.setClosureDate(date);
        c.setReason(request.reason().trim());
        return CanteenClosureDto.from(closureRepository.save(c));
    }

    @Transactional
    public void removeClosure(Long id) {
        if (!closureRepository.existsById(id)) throw new ResourceNotFoundException("Closure not found");
        closureRepository.deleteById(id);
    }

    private LocalTime parseTime(String value) {
        try { return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5)); }
        catch (Exception e) { throw new BadRequestException("Time must use HH:mm format"); }
    }
    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value); }
        catch (Exception e) { throw new BadRequestException("Date must use YYYY-MM-DD format"); }
    }
}
