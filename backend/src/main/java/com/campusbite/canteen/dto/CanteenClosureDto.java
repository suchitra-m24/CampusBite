package com.campusbite.canteen.dto;

import com.campusbite.canteen.CanteenClosure;

public record CanteenClosureDto(Long id, String date, String reason) {
    public static CanteenClosureDto from(CanteenClosure c) {
        return new CanteenClosureDto(c.getId(), c.getClosureDate().toString(), c.getReason());
    }
}
